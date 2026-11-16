package com.dzfashion.payment.service;

import com.dzfashion.payment.dto.request.CreateCodSettlementBatchRequest;
import com.dzfashion.payment.dto.response.CodSettlementBatchResponse;
import com.dzfashion.payment.entity.CodSettlementBatchEntity;
import com.dzfashion.payment.entity.OutboxEventEntity;
import com.dzfashion.payment.enums.CodSettlementStatus;
import com.dzfashion.payment.exception.InvalidOperationException;
import com.dzfashion.payment.repository.CodSettlementBatchRepository;
import com.dzfashion.payment.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CodSettlementServiceTest {

    @Mock
    private CodSettlementBatchRepository codSettlementBatchRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private CodSettlementService service;

    @BeforeEach
    void setUp() {
        service = new CodSettlementService(codSettlementBatchRepository, outboxEventRepository, objectMapper);
    }

    @Test
    @DisplayName("createBatch with discrepancy between collected and remitted flags status DISCREPANCY_FLAGGED")
    void createBatch_WithDiscrepancy_FlagsStatus() {
        UUID staffId = UUID.randomUUID();
        CreateCodSettlementBatchRequest request = new CreateCodSettlementBatchRequest(
                "Delhivery Logistics",
                LocalDate.now(),
                new BigDecimal("15000.00"),
                new BigDecimal("14500.00"), // ₹500 shortage
                "Cash shortage reported by delivery agent"
        );

        when(codSettlementBatchRepository.save(any(CodSettlementBatchEntity.class)))
                .thenAnswer(inv -> {
                    CodSettlementBatchEntity e = inv.getArgument(0);
                    e.setId(UUID.randomUUID());
                    return e;
                });

        CodSettlementBatchResponse response = service.createBatch(request, staffId);

        assertThat(response.status()).isEqualTo(CodSettlementStatus.DISCREPANCY_FLAGGED);
        assertThat(response.discrepancyAmount()).isEqualByComparingTo("-500.00");
        assertThat(response.batchReference()).startsWith("COD-");
        verify(outboxEventRepository).save(any(OutboxEventEntity.class));
    }

    @Test
    @DisplayName("createBatch with matched amounts sets status SUBMITTED")
    void createBatch_MatchedAmounts_SubmittedStatus() {
        UUID staffId = UUID.randomUUID();
        CreateCodSettlementBatchRequest request = new CreateCodSettlementBatchRequest(
                "BlueDart Express",
                LocalDate.now(),
                new BigDecimal("20000.00"),
                new BigDecimal("20000.00"),
                "Fully reconciled"
        );

        when(codSettlementBatchRepository.save(any(CodSettlementBatchEntity.class)))
                .thenAnswer(inv -> {
                    CodSettlementBatchEntity e = inv.getArgument(0);
                    e.setId(UUID.randomUUID());
                    return e;
                });

        CodSettlementBatchResponse response = service.createBatch(request, staffId);

        assertThat(response.status()).isEqualTo(CodSettlementStatus.SUBMITTED);
        assertThat(response.discrepancyAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("approveBatch transitions batch to APPROVED by Owner")
    void approveBatch_OwnerApproves_Success() {
        UUID batchId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        CodSettlementBatchEntity batch = new CodSettlementBatchEntity(
                "COD-20261116-ABC",
                "Delhivery",
                LocalDate.now(),
                new BigDecimal("10000.00"),
                new BigDecimal("10000.00"),
                BigDecimal.ZERO,
                UUID.randomUUID(),
                "Ready for owner review"
        );
        batch.setId(batchId);
        batch.setStatus(CodSettlementStatus.SUBMITTED);

        when(codSettlementBatchRepository.findById(batchId)).thenReturn(Optional.of(batch));
        when(codSettlementBatchRepository.save(any(CodSettlementBatchEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        CodSettlementBatchResponse response = service.approveBatch(batchId, ownerId);

        assertThat(response.status()).isEqualTo(CodSettlementStatus.APPROVED);
        assertThat(response.approvedBy()).isEqualTo(ownerId);
        assertThat(response.approvedAt()).isNotNull();
        verify(outboxEventRepository).save(any(OutboxEventEntity.class));
    }

    @Test
    @DisplayName("approveBatch on already approved batch throws InvalidOperationException")
    void approveBatch_AlreadyApproved_ThrowsException() {
        UUID batchId = UUID.randomUUID();
        CodSettlementBatchEntity batch = new CodSettlementBatchEntity();
        batch.setStatus(CodSettlementStatus.APPROVED);

        when(codSettlementBatchRepository.findById(batchId)).thenReturn(Optional.of(batch));

        assertThatThrownBy(() -> service.approveBatch(batchId, UUID.randomUUID()))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("already approved");
    }
}
