package com.dzfashion.fulfillment.service;

import com.dzfashion.fulfillment.dto.request.CreateAgencyCodSettlementRequest;
import com.dzfashion.fulfillment.dto.response.AgencyCodSettlementResponse;
import com.dzfashion.fulfillment.entity.AgencyCodSettlementEntity;
import com.dzfashion.fulfillment.entity.ShipmentEntity;
import com.dzfashion.fulfillment.entity.TransportAgencyEntity;
import com.dzfashion.fulfillment.enums.AgencyCodSettlementStatus;
import com.dzfashion.fulfillment.enums.ShipmentStatus;
import com.dzfashion.fulfillment.exception.InvalidOperationException;
import com.dzfashion.fulfillment.repository.AgencyCodSettlementRepository;
import com.dzfashion.fulfillment.repository.OutboxEventRepository;
import com.dzfashion.fulfillment.repository.ShipmentRepository;
import com.dzfashion.fulfillment.repository.TransportAgencyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgencyCodSettlementServiceTest {

    @Mock
    private AgencyCodSettlementRepository agencyCodSettlementRepository;

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private TransportAgencyRepository transportAgencyRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ShipmentService shipmentService;

    private ObjectMapper objectMapper;
    private AgencyCodSettlementService agencyCodSettlementService;

    private TransportAgencyEntity agency;
    private UUID agencyId;
    private ShipmentEntity shipment1;
    private ShipmentEntity shipment2;
    private UUID shipmentId1;
    private UUID shipmentId2;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        agencyCodSettlementService = new AgencyCodSettlementService(
                agencyCodSettlementRepository,
                shipmentRepository,
                transportAgencyRepository,
                outboxEventRepository,
                shipmentService,
                objectMapper
        );

        agencyId = UUID.randomUUID();
        agency = new TransportAgencyEntity("Delhivery", "Hub Lead", "9876543210");
        agency.setId(agencyId);

        shipmentId1 = UUID.randomUUID();
        shipment1 = new ShipmentEntity(
                UUID.randomUUID(), "ORD-001", UUID.randomUUID(), "Customer A",
                "9876543210", "Address A", "Surat", "Gujarat", "395002", true, new BigDecimal("2000.00")
        );
        shipment1.setId(shipmentId1);
        shipment1.setStatus(ShipmentStatus.DELIVERED);
        shipment1.setCodCollectedAmount(new BigDecimal("2000.00"));
        shipment1.setTransportAgency(agency);

        shipmentId2 = UUID.randomUUID();
        shipment2 = new ShipmentEntity(
                UUID.randomUUID(), "ORD-002", UUID.randomUUID(), "Customer B",
                "9123456789", "Address B", "Surat", "Gujarat", "395002", true, new BigDecimal("1500.00")
        );
        shipment2.setId(shipmentId2);
        shipment2.setStatus(ShipmentStatus.DELIVERED);
        shipment2.setCodCollectedAmount(new BigDecimal("1500.00"));
        shipment2.setTransportAgency(agency);
    }

    @Test
    @DisplayName("Should prepare COD settlement batch with discrepancy calculation and tag shipments")
    void testPrepareSettlement_Success() {
        when(transportAgencyRepository.findById(agencyId)).thenReturn(Optional.of(agency));
        when(shipmentRepository.findById(shipmentId1)).thenReturn(Optional.of(shipment1));
        when(shipmentRepository.findById(shipmentId2)).thenReturn(Optional.of(shipment2));
        when(agencyCodSettlementRepository.save(any(AgencyCodSettlementEntity.class))).thenAnswer(inv -> {
            AgencyCodSettlementEntity entity = inv.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        // Total expected = 2000 + 1500 = 3500. Agency remitted 3400 (short 100)
        CreateAgencyCodSettlementRequest request = new CreateAgencyCodSettlementRequest(
                agencyId,
                LocalDate.of(2026, 11, 16),
                new BigDecimal("3400.00"),
                "NEFT-REF-778899",
                "Weekly Surat hub reconciliation",
                List.of(shipmentId1, shipmentId2)
        );

        AgencyCodSettlementResponse response = agencyCodSettlementService.prepareSettlement(request, UUID.randomUUID());

        assertThat(response).isNotNull();
        assertThat(response.batchReference()).startsWith("COD-SETTLE-20261116-");
        assertThat(response.totalDeliveredOrders()).isEqualTo(2);
        assertThat(response.totalExpectedCod()).isEqualByComparingTo("3500.00");
        assertThat(response.remittedAmount()).isEqualByComparingTo("3400.00");
        assertThat(response.discrepancyAmount()).isEqualByComparingTo("-100.00"); // Short remittance
        assertThat(response.status()).isEqualTo(AgencyCodSettlementStatus.PREPARED);

        assertThat(shipment1.getCodSettlementBatchId()).isNotNull();
        assertThat(shipment2.getCodSettlementBatchId()).isNotNull();
        verify(outboxEventRepository).save(any());
    }

    @Test
    @DisplayName("Should reject settlement when an order has already been included in a settlement batch")
    void testPrepareSettlement_DoubleSettlementPrevention() {
        shipment1.setCodSettlementBatchId(UUID.randomUUID()); // Already settled!

        when(transportAgencyRepository.findById(agencyId)).thenReturn(Optional.of(agency));
        when(shipmentRepository.findById(shipmentId1)).thenReturn(Optional.of(shipment1));

        CreateAgencyCodSettlementRequest request = new CreateAgencyCodSettlementRequest(
                agencyId,
                LocalDate.of(2026, 11, 16),
                new BigDecimal("2000.00"),
                "NEFT-REF-778899",
                "Notes",
                List.of(shipmentId1)
        );

        assertThatThrownBy(() -> agencyCodSettlementService.prepareSettlement(request, UUID.randomUUID()))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("already included in COD settlement batch");
    }

    @Test
    @DisplayName("Should reject settlement if shipment is not DELIVERED")
    void testPrepareSettlement_NonDeliveredShipmentRejected() {
        shipment1.setStatus(ShipmentStatus.DISPATCHED);

        when(transportAgencyRepository.findById(agencyId)).thenReturn(Optional.of(agency));
        when(shipmentRepository.findById(shipmentId1)).thenReturn(Optional.of(shipment1));

        CreateAgencyCodSettlementRequest request = new CreateAgencyCodSettlementRequest(
                agencyId,
                LocalDate.of(2026, 11, 16),
                new BigDecimal("2000.00"),
                "NEFT-REF-778899",
                "Notes",
                List.of(shipmentId1)
        );

        assertThatThrownBy(() -> agencyCodSettlementService.prepareSettlement(request, UUID.randomUUID()))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Only DELIVERED shipments can be settled");
    }

    @Test
    @DisplayName("Should reject settlement if shipment belongs to a different transport agency")
    void testPrepareSettlement_DifferentAgencyShipmentRejected() {
        TransportAgencyEntity differentAgency = new TransportAgencyEntity("BlueDart", "Lead", "9876500000");
        differentAgency.setId(UUID.randomUUID());
        shipment1.setTransportAgency(differentAgency);

        when(transportAgencyRepository.findById(agencyId)).thenReturn(Optional.of(agency));
        when(shipmentRepository.findById(shipmentId1)).thenReturn(Optional.of(shipment1));

        CreateAgencyCodSettlementRequest request = new CreateAgencyCodSettlementRequest(
                agencyId,
                LocalDate.of(2026, 11, 16),
                new BigDecimal("2000.00"),
                "NEFT-REF-778899",
                "Notes",
                List.of(shipmentId1)
        );

        assertThatThrownBy(() -> agencyCodSettlementService.prepareSettlement(request, UUID.randomUUID()))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("does not belong to transport agency");
    }

    @Test
    @DisplayName("Should allow Owner to approve settlement batch, locking status to APPROVED")
    void testApproveSettlement_OwnerSuccess() {
        UUID settlementId = UUID.randomUUID();
        AgencyCodSettlementEntity settlement = new AgencyCodSettlementEntity(
                "COD-SETTLE-20261116-0001",
                agency,
                LocalDate.of(2026, 11, 16),
                2,
                new BigDecimal("3500.00"),
                new BigDecimal("3500.00"),
                BigDecimal.ZERO,
                UUID.randomUUID(),
                "PROOF-123",
                "All matched"
        );
        settlement.setId(settlementId);

        when(agencyCodSettlementRepository.findById(settlementId)).thenReturn(Optional.of(settlement));
        when(agencyCodSettlementRepository.save(any(AgencyCodSettlementEntity.class))).thenReturn(settlement);

        UUID ownerId = UUID.randomUUID();
        AgencyCodSettlementResponse response = agencyCodSettlementService.approveSettlement(settlementId, ownerId);

        assertThat(response.status()).isEqualTo(AgencyCodSettlementStatus.APPROVED);
        assertThat(settlement.getApprovedBy()).isEqualTo(ownerId);
        assertThat(settlement.getApprovedAt()).isNotNull();
        verify(outboxEventRepository).save(any());
    }

    @Test
    @DisplayName("Should reject approval of already approved settlement batch")
    void testApproveSettlement_AlreadyApproved() {
        UUID settlementId = UUID.randomUUID();
        AgencyCodSettlementEntity settlement = new AgencyCodSettlementEntity();
        settlement.setStatus(AgencyCodSettlementStatus.APPROVED);
        settlement.setBatchReference("COD-SETTLE-APPROVED");

        when(agencyCodSettlementRepository.findById(settlementId)).thenReturn(Optional.of(settlement));

        assertThatThrownBy(() -> agencyCodSettlementService.approveSettlement(settlementId, UUID.randomUUID()))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("is already approved");
    }
}
