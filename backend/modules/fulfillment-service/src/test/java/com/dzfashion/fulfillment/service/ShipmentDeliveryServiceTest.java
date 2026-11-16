package com.dzfashion.fulfillment.service;

import com.dzfashion.fulfillment.config.FulfillmentProperties;
import com.dzfashion.fulfillment.dto.request.ReceiveReturnAtWarehouseRequest;
import com.dzfashion.fulfillment.dto.request.RecordDeliveryAttemptRequest;
import com.dzfashion.fulfillment.dto.request.RecordDeliverySuccessRequest;
import com.dzfashion.fulfillment.dto.response.ShipmentResponse;
import com.dzfashion.fulfillment.entity.ShipmentEntity;
import com.dzfashion.fulfillment.enums.DeliveryAttemptResult;
import com.dzfashion.fulfillment.enums.ShipmentStatus;
import com.dzfashion.fulfillment.exception.InvalidOperationException;
import com.dzfashion.fulfillment.repository.DeliveryAttemptLogRepository;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShipmentDeliveryServiceTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private DeliveryAttemptLogRepository deliveryAttemptLogRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private FulfillmentProperties fulfillmentProperties;

    @Mock
    private ShipmentService shipmentService;

    private ObjectMapper objectMapper;
    private ShipmentDeliveryService shipmentDeliveryService;

    private ShipmentEntity shipment;
    private UUID shipmentId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        shipmentDeliveryService = new ShipmentDeliveryService(
                shipmentRepository,
                deliveryAttemptLogRepository,
                outboxEventRepository,
                fulfillmentProperties,
                shipmentService,
                objectMapper
        );

        lenient().when(fulfillmentProperties.getMaxDeliveryAttempts()).thenReturn(2);
        lenient().when(fulfillmentProperties.getReturnWindowDays()).thenReturn(3);
        lenient().when(fulfillmentProperties.getWarehouseName()).thenReturn("Surat Central Warehouse");

        shipmentId = UUID.randomUUID();
        shipment = new ShipmentEntity(
                UUID.randomUUID(), "ORD-20261116-0001", UUID.randomUUID(), "Ritu Patel",
                "9876543210", "Ghod Dod Road", "Surat", "Gujarat", "395007", true, new BigDecimal("1850.00")
        );
        shipment.setId(shipmentId);
        shipment.setStatus(ShipmentStatus.DISPATCHED);
        shipment.setPackedAt(Instant.now());
    }

    @Test
    @DisplayName("First failed attempt should set status to DELIVERY_ATTEMPTED_1 and attempts to 1")
    void testRecordDeliveryAttempt_FirstAttempt() {
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(any(ShipmentEntity.class))).thenReturn(shipment);
        when(shipmentService.mapToResponse(any())).thenReturn(null);

        RecordDeliveryAttemptRequest request = new RecordDeliveryAttemptRequest(
                Instant.now(), DeliveryAttemptResult.CUSTOMER_UNAVAILABLE, "Customer out of town until evening"
        );

        shipmentDeliveryService.recordDeliveryAttempt(shipmentId, request, UUID.randomUUID());

        assertThat(shipment.getDeliveryAttempts()).isEqualTo(1);
        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.DELIVERY_ATTEMPTED_1);
        assertThat(shipment.getAttemptLogs()).hasSize(1);
        assertThat(shipment.getAttemptLogs().get(0).getResult()).isEqualTo(DeliveryAttemptResult.CUSTOMER_UNAVAILABLE);
        verify(outboxEventRepository).save(any());
    }

    @Test
    @DisplayName("Second failed attempt should trigger RETURNING_TO_WAREHOUSE inbound return")
    void testRecordDeliveryAttempt_SecondAttempt_TriggersReturn() {
        shipment.setDeliveryAttempts(1);
        shipment.setStatus(ShipmentStatus.DELIVERY_ATTEMPTED_1);

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(any(ShipmentEntity.class))).thenReturn(shipment);
        when(shipmentService.mapToResponse(any())).thenReturn(null);

        RecordDeliveryAttemptRequest request = new RecordDeliveryAttemptRequest(
                Instant.now(), DeliveryAttemptResult.DOOR_LOCKED, "Premises locked on 2nd attempt"
        );

        shipmentDeliveryService.recordDeliveryAttempt(shipmentId, request, UUID.randomUUID());

        assertThat(shipment.getDeliveryAttempts()).isEqualTo(2);
        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.RETURNING_TO_WAREHOUSE);
        verify(outboxEventRepository).save(any());
    }

    @Test
    @DisplayName("Third delivery attempt should be strictly rejected (maximum 2 attempts invariant)")
    void testRecordDeliveryAttempt_ThirdAttempt_StrictlyRejected() {
        shipment.setDeliveryAttempts(2);
        shipment.setStatus(ShipmentStatus.RETURNING_TO_WAREHOUSE);

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));

        RecordDeliveryAttemptRequest request = new RecordDeliveryAttemptRequest(
                Instant.now(), DeliveryAttemptResult.CUSTOMER_REFUSED, "3rd attempt"
        );

        assertThatThrownBy(() -> shipmentDeliveryService.recordDeliveryAttempt(shipmentId, request, UUID.randomUUID()))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("returning or returned to warehouse");
    }

    @Test
    @DisplayName("Successful delivery should mark DELIVERED, record collected COD, and set 3-day return window")
    void testRecordDeliverySuccess() {
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(any(ShipmentEntity.class))).thenReturn(shipment);
        when(shipmentService.mapToResponse(any())).thenReturn(null);

        RecordDeliverySuccessRequest request = new RecordDeliverySuccessRequest(
                LocalDate.of(2026, 11, 17),
                new BigDecimal("1850.00"),
                "Handed over to customer"
        );

        shipmentDeliveryService.recordDeliverySuccess(shipmentId, request, UUID.randomUUID());

        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.DELIVERED);
        assertThat(shipment.getDeliveryDate()).isEqualTo(LocalDate.of(2026, 11, 17));
        assertThat(shipment.getCodCollectedAmount()).isEqualByComparingTo("1850.00");
        assertThat(shipment.getEligibleReturnUntil()).isNotNull();
        verify(outboxEventRepository).save(any());
    }

    @Test
    @DisplayName("Should reject delivery success if shipment is returning to warehouse")
    void testRecordDeliverySuccess_ReturningToWarehouse_Rejected() {
        shipment.setStatus(ShipmentStatus.RETURNING_TO_WAREHOUSE);
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));

        RecordDeliverySuccessRequest request = new RecordDeliverySuccessRequest(
                LocalDate.now(), new BigDecimal("1850.00"), "Delivered"
        );

        assertThatThrownBy(() -> shipmentDeliveryService.recordDeliverySuccess(shipmentId, request, UUID.randomUUID()))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("already returning or returned to warehouse");
    }

    @Test
    @DisplayName("Should receive return at Surat warehouse and record condition notes")
    void testReceiveReturnAtWarehouse_Success() {
        shipment.setStatus(ShipmentStatus.RETURNING_TO_WAREHOUSE);

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(any(ShipmentEntity.class))).thenReturn(shipment);
        when(shipmentService.mapToResponse(any())).thenReturn(null);

        ReceiveReturnAtWarehouseRequest request = new ReceiveReturnAtWarehouseRequest("Outer parcel intact, tag intact, ready for restocking");
        shipmentDeliveryService.receiveReturnAtWarehouse(shipmentId, request, UUID.randomUUID());

        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.RETURNED_TO_WAREHOUSE);
        assertThat(shipment.getReturnedToWarehouseAt()).isNotNull();
        assertThat(shipment.getNotes()).contains("Outer parcel intact");
        verify(outboxEventRepository).save(any());
    }
}
