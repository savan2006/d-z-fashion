package com.dzfashion.fulfillment.service;

import com.dzfashion.fulfillment.dto.request.CreateShipmentItemRequest;
import com.dzfashion.fulfillment.dto.request.CreateShipmentRequest;
import com.dzfashion.fulfillment.dto.response.CustomerTrackingResponse;
import com.dzfashion.fulfillment.dto.response.ShipmentResponse;
import com.dzfashion.fulfillment.entity.ShipmentEntity;
import com.dzfashion.fulfillment.entity.TransportAgencyEntity;
import com.dzfashion.fulfillment.enums.CustomerShipmentStatus;
import com.dzfashion.fulfillment.enums.ShipmentStatus;
import com.dzfashion.fulfillment.exception.InvalidOperationException;
import com.dzfashion.fulfillment.exception.ResourceConflictException;
import com.dzfashion.fulfillment.exception.ResourceNotFoundException;
import com.dzfashion.fulfillment.repository.OutboxEventRepository;
import com.dzfashion.fulfillment.repository.ShipmentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShipmentServiceTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private ObjectMapper objectMapper;
    private ShipmentService shipmentService;

    private UUID shipmentId;
    private UUID orderId;
    private UUID customerId;
    private ShipmentEntity shipment;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        shipmentService = new ShipmentService(shipmentRepository, outboxEventRepository, objectMapper);

        shipmentId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        customerId = UUID.randomUUID();

        shipment = new ShipmentEntity(
                orderId,
                "ORD-20261116-0001",
                customerId,
                "Ayesha Khan",
                "+91 9876543210",
                "Flat 402, Lotus Apartments, Bandra West",
                "Mumbai",
                "Maharashtra",
                "400050",
                true,
                new BigDecimal("2499.00")
        );
        shipment.setId(shipmentId);
    }

    @Test
    @DisplayName("Should create shipment and line items successfully")
    void testCreateShipment_Success() {
        CreateShipmentItemRequest item = new CreateShipmentItemRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Pure Silk Embroidered Anarkali",
                "ANARKALI-RED-M",
                "M",
                "Wine Red",
                "BIN-A-12",
                2
        );
        CreateShipmentRequest request = new CreateShipmentRequest(
                orderId,
                "ORD-20261116-0001",
                customerId,
                "Ayesha Khan",
                "+91 9876543210",
                "Flat 402, Lotus Apartments",
                "Mumbai",
                "Maharashtra",
                "400050",
                true,
                new BigDecimal("2499.00"),
                List.of(item)
        );

        when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(shipmentRepository.save(any(ShipmentEntity.class))).thenAnswer(inv -> {
            ShipmentEntity s = inv.getArgument(0);
            s.setId(shipmentId);
            return s;
        });

        ShipmentResponse response = shipmentService.createShipment(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(shipmentId);
        assertThat(response.orderNumber()).isEqualTo("ORD-20261116-0001");
        assertThat(response.status()).isEqualTo(ShipmentStatus.PROCESSING);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).sku()).isEqualTo("ANARKALI-RED-M");
        assertThat(response.items().get(0).binLocation()).isEqualTo("BIN-A-12");
        verify(outboxEventRepository).save(any());
    }

    @Test
    @DisplayName("Should reject duplicate shipment creation for same order ID")
    void testCreateShipment_DuplicateOrderId() {
        CreateShipmentRequest request = new CreateShipmentRequest(
                orderId, "ORD-20261116-0001", customerId, "Ayesha", "9876543210",
                "Address", "Mumbai", "Maharashtra", "400050", false, BigDecimal.ZERO, List.of()
        );
        when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.of(shipment));

        assertThatThrownBy(() -> shipmentService.createShipment(request))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Should mark shipment and items as picked and packed")
    void testPickAndPackShipment_Success() {
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(any(ShipmentEntity.class))).thenReturn(shipment);

        UUID staffId = UUID.randomUUID();
        ShipmentResponse response = shipmentService.pickAndPackShipment(shipmentId, staffId);

        assertThat(response).isNotNull();
        assertThat(shipment.getPickedAt()).isNotNull();
        assertThat(shipment.getPackedAt()).isNotNull();
        verify(outboxEventRepository).save(any());
    }

    @Test
    @DisplayName("Should reject picking and packing when shipment is not in PROCESSING status")
    void testPickAndPackShipment_InvalidStatus() {
        shipment.setStatus(ShipmentStatus.DISPATCHED);
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));

        assertThatThrownBy(() -> shipmentService.pickAndPackShipment(shipmentId, UUID.randomUUID()))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Cannot pick and pack shipment in status: DISPATCHED");
    }

    @Test
    @DisplayName("Should return customer tracking with sanitized customer status and no internal notes")
    void testGetCustomerTracking_Sanitized() {
        TransportAgencyEntity agency = new TransportAgencyEntity("Delhivery", "Lead", "9876543210");
        shipment.setTransportAgency(agency);
        shipment.setTrackingReference("DEL-SURAT-9988");
        shipment.setStatus(ShipmentStatus.DISPATCHED);
        shipment.setNotes("Internal note: Customer requested morning delivery, sensitive fabric");

        when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.of(shipment));

        CustomerTrackingResponse tracking = shipmentService.getCustomerTracking(orderId);

        assertThat(tracking.orderId()).isEqualTo(orderId);
        assertThat(tracking.orderNumber()).isEqualTo("ORD-20261116-0001");
        assertThat(tracking.customerStatus()).isEqualTo(CustomerShipmentStatus.SHIPPED);
        assertThat(tracking.transportAgencyName()).isEqualTo("Delhivery");
        assertThat(tracking.trackingReference()).isEqualTo("DEL-SURAT-9988");
    }
}
