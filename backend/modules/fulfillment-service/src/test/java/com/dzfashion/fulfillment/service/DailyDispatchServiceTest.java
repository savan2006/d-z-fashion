package com.dzfashion.fulfillment.service;

import com.dzfashion.fulfillment.dto.request.CreateDispatchBatchRequest;
import com.dzfashion.fulfillment.dto.request.RecordHandoverRequest;
import com.dzfashion.fulfillment.dto.response.DailyDispatchBatchResponse;
import com.dzfashion.fulfillment.entity.DailyDispatchBatchEntity;
import com.dzfashion.fulfillment.entity.ShipmentEntity;
import com.dzfashion.fulfillment.entity.TransportAgencyEntity;
import com.dzfashion.fulfillment.enums.DailyDispatchStatus;
import com.dzfashion.fulfillment.enums.ShipmentStatus;
import com.dzfashion.fulfillment.exception.InvalidOperationException;
import com.dzfashion.fulfillment.repository.DailyDispatchBatchRepository;
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
import java.time.Instant;
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
class DailyDispatchServiceTest {

    @Mock
    private DailyDispatchBatchRepository dailyDispatchBatchRepository;

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private TransportAgencyRepository transportAgencyRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private ObjectMapper objectMapper;
    private DailyDispatchService dailyDispatchService;

    private TransportAgencyEntity agency;
    private UUID agencyId;
    private ShipmentEntity shipment;
    private UUID shipmentId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        dailyDispatchService = new DailyDispatchService(
                dailyDispatchBatchRepository,
                shipmentRepository,
                transportAgencyRepository,
                outboxEventRepository,
                objectMapper
        );

        agencyId = UUID.randomUUID();
        agency = new TransportAgencyEntity("Delhivery", "Hub Lead", "9876543210");
        agency.setId(agencyId);

        shipmentId = UUID.randomUUID();
        shipment = new ShipmentEntity(
                UUID.randomUUID(), "ORD-20261116-0001", UUID.randomUUID(), "Ayesha", "9876543210",
                "Address", "Surat", "Gujarat", "395002", true, new BigDecimal("1000.00")
        );
        shipment.setId(shipmentId);
    }

    @Test
    @DisplayName("Should create daily dispatch batch with packed orders")
    void testCreateDailyDispatchBatch_Success() {
        shipment.setPackedAt(Instant.now()); // Packed!

        when(transportAgencyRepository.findById(agencyId)).thenReturn(Optional.of(agency));
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));
        when(dailyDispatchBatchRepository.save(any(DailyDispatchBatchEntity.class))).thenAnswer(inv -> {
            DailyDispatchBatchEntity b = inv.getArgument(0);
            b.setId(UUID.randomUUID());
            return b;
        });

        CreateDispatchBatchRequest request = new CreateDispatchBatchRequest(
                LocalDate.of(2026, 11, 16),
                agencyId,
                "BILL-REF-101",
                "Morning batch",
                List.of(shipmentId)
        );

        DailyDispatchBatchResponse response = dailyDispatchService.createDailyDispatchBatch(request, UUID.randomUUID());

        assertThat(response).isNotNull();
        assertThat(response.batchNumber()).startsWith("DISPATCH-20261116-");
        assertThat(response.parcelCount()).isEqualTo(1);
        assertThat(response.status()).isEqualTo(DailyDispatchStatus.PREPARED);
        assertThat(shipment.getDailyDispatchBatch()).isNotNull();
        verify(outboxEventRepository).save(any());
    }

    @Test
    @DisplayName("Should reject dispatching shipment that has not been packed yet")
    void testCreateDailyDispatchBatch_UnpackedOrderRejected() {
        shipment.setPackedAt(null); // Not packed!

        when(transportAgencyRepository.findById(agencyId)).thenReturn(Optional.of(agency));
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));

        CreateDispatchBatchRequest request = new CreateDispatchBatchRequest(
                LocalDate.of(2026, 11, 16),
                agencyId,
                "BILL-REF-101",
                "Morning batch",
                List.of(shipmentId)
        );

        assertThatThrownBy(() -> dailyDispatchService.createDailyDispatchBatch(request, UUID.randomUUID()))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("has not been packed yet");
    }

    @Test
    @DisplayName("Should reject order already assigned to another dispatch batch")
    void testCreateDailyDispatchBatch_AlreadyAssignedOrderRejected() {
        shipment.setPackedAt(Instant.now());
        DailyDispatchBatchEntity existingBatch = new DailyDispatchBatchEntity();
        existingBatch.setBatchNumber("DISPATCH-EXISTING-001");
        shipment.setDailyDispatchBatch(existingBatch);

        when(transportAgencyRepository.findById(agencyId)).thenReturn(Optional.of(agency));
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));

        CreateDispatchBatchRequest request = new CreateDispatchBatchRequest(
                LocalDate.of(2026, 11, 16),
                agencyId,
                "BILL-REF-101",
                "Morning batch",
                List.of(shipmentId)
        );

        assertThatThrownBy(() -> dailyDispatchService.createDailyDispatchBatch(request, UUID.randomUUID()))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("already assigned to dispatch batch");
    }

    @Test
    @DisplayName("Should reject creating batch with inactive transport agency")
    void testCreateDailyDispatchBatch_InactiveAgencyRejected() {
        agency.setActive(false);
        when(transportAgencyRepository.findById(agencyId)).thenReturn(Optional.of(agency));

        CreateDispatchBatchRequest request = new CreateDispatchBatchRequest(
                LocalDate.of(2026, 11, 16),
                agencyId,
                "BILL-REF-101",
                "Morning batch",
                List.of(shipmentId)
        );

        assertThatThrownBy(() -> dailyDispatchService.createDailyDispatchBatch(request, UUID.randomUUID()))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("is inactive");
    }

    @Test
    @DisplayName("Should record agency handover and transition shipments to DISPATCHED")
    void testRecordAgencyHandover_Success() {
        UUID batchId = UUID.randomUUID();
        DailyDispatchBatchEntity batch = new DailyDispatchBatchEntity(
                "DISPATCH-20261116-0001",
                LocalDate.of(2026, 11, 16),
                agency,
                UUID.randomUUID(),
                1,
                "BILL-123",
                "Notes"
        );
        batch.setId(batchId);

        when(dailyDispatchBatchRepository.findById(batchId)).thenReturn(Optional.of(batch));
        when(transportAgencyRepository.findById(agencyId)).thenReturn(Optional.of(agency));
        when(dailyDispatchBatchRepository.save(any(DailyDispatchBatchEntity.class))).thenReturn(batch);
        when(shipmentRepository.findByDailyDispatchBatchId(batchId)).thenReturn(List.of(shipment));

        RecordHandoverRequest request = new RecordHandoverRequest(agencyId, "WAYBILL-999", Instant.now(), "Driver Ramesh arrived at 10:30 AM");
        DailyDispatchBatchResponse response = dailyDispatchService.recordAgencyHandover(batchId, request, UUID.randomUUID());

        assertThat(response.status()).isEqualTo(DailyDispatchStatus.HANDED_OVER);
        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.DISPATCHED);
        assertThat(shipment.getDispatchedAt()).isNotNull();
        assertThat(shipment.getDispatchDate()).isEqualTo(LocalDate.of(2026, 11, 16));
    }

    @Test
    @DisplayName("Should reject handover of already handed-over batch")
    void testRecordAgencyHandover_AlreadyHandedOver() {
        UUID batchId = UUID.randomUUID();
        DailyDispatchBatchEntity batch = new DailyDispatchBatchEntity();
        batch.setStatus(DailyDispatchStatus.HANDED_OVER);
        batch.setBatchNumber("DISPATCH-DONE");

        when(dailyDispatchBatchRepository.findById(batchId)).thenReturn(Optional.of(batch));

        RecordHandoverRequest request = new RecordHandoverRequest(agencyId, "WAYBILL-999", Instant.now(), "Driver Ramesh");

        assertThatThrownBy(() -> dailyDispatchService.recordAgencyHandover(batchId, request, UUID.randomUUID()))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("cannot be handed over");
    }
}
