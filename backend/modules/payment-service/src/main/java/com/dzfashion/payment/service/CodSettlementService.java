package com.dzfashion.payment.service;

import com.dzfashion.payment.dto.request.CreateCodSettlementBatchRequest;
import com.dzfashion.payment.dto.response.CodSettlementBatchResponse;
import com.dzfashion.payment.entity.CodSettlementBatchEntity;
import com.dzfashion.payment.entity.OutboxEventEntity;
import com.dzfashion.payment.enums.CodSettlementStatus;
import com.dzfashion.payment.exception.InvalidOperationException;
import com.dzfashion.payment.exception.ResourceNotFoundException;
import com.dzfashion.payment.repository.CodSettlementBatchRepository;
import com.dzfashion.payment.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class CodSettlementService {

    private static final Logger log = LoggerFactory.getLogger(CodSettlementService.class);

    private final CodSettlementBatchRepository codSettlementBatchRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public CodSettlementService(CodSettlementBatchRepository codSettlementBatchRepository,
                                OutboxEventRepository outboxEventRepository,
                                ObjectMapper objectMapper) {
        this.codSettlementBatchRepository = codSettlementBatchRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    public CodSettlementBatchResponse createBatch(CreateCodSettlementBatchRequest request, UUID staffId) {
        String datePart = request.settlementDate().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
        String batchReference = "COD-" + datePart + "-" + suffix;

        BigDecimal collected = request.totalCollectedAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal remitted = request.remittedAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal discrepancy = remitted.subtract(collected).setScale(2, RoundingMode.HALF_UP);

        CodSettlementStatus initialStatus = discrepancy.compareTo(BigDecimal.ZERO) != 0
                ? CodSettlementStatus.DISCREPANCY_FLAGGED
                : CodSettlementStatus.SUBMITTED;

        CodSettlementBatchEntity entity = new CodSettlementBatchEntity(
                batchReference,
                request.transportAgencyName().trim(),
                request.settlementDate(),
                collected,
                remitted,
                discrepancy,
                staffId,
                request.notes() != null ? request.notes().trim() : null
        );
        entity.setStatus(initialStatus);

        CodSettlementBatchEntity saved = codSettlementBatchRepository.save(entity);
        saveOutboxEvent("COD_SETTLEMENT", saved.getId().toString(), "COD_SETTLEMENT_SUBMITTED", saved);
        log.info("Created COD settlement batch {} by staff {} with discrepancy ₹{}",
                saved.getBatchReference(), staffId, discrepancy);
        return mapToResponse(saved);
    }

    public CodSettlementBatchResponse approveBatch(UUID batchId, UUID ownerId) {
        CodSettlementBatchEntity entity = codSettlementBatchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("COD settlement batch not found with ID: " + batchId));

        if (entity.getStatus() == CodSettlementStatus.APPROVED) {
            throw new InvalidOperationException("COD settlement batch is already approved");
        }

        entity.setStatus(CodSettlementStatus.APPROVED);
        entity.setApprovedBy(ownerId);
        entity.setApprovedAt(Instant.now());

        CodSettlementBatchEntity saved = codSettlementBatchRepository.save(entity);
        saveOutboxEvent("COD_SETTLEMENT", saved.getId().toString(), "COD_SETTLEMENT_APPROVED", saved);
        log.info("Owner {} approved COD settlement batch {} (ID: {})", ownerId, saved.getBatchReference(), saved.getId());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public CodSettlementBatchResponse getBatchById(UUID batchId) {
        CodSettlementBatchEntity entity = codSettlementBatchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("COD settlement batch not found with ID: " + batchId));
        return mapToResponse(entity);
    }

    @Transactional(readOnly = true)
    public CodSettlementBatchResponse getBatchByReference(String batchReference) {
        CodSettlementBatchEntity entity = codSettlementBatchRepository.findByBatchReference(batchReference.trim())
                .orElseThrow(() -> new ResourceNotFoundException("COD settlement batch not found with reference: " + batchReference));
        return mapToResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<CodSettlementBatchResponse> getBatchesByStatus(CodSettlementStatus status) {
        return codSettlementBatchRepository.findByStatusOrderBySettlementDateDesc(status).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<CodSettlementBatchResponse> getAllBatches(Pageable pageable) {
        return codSettlementBatchRepository.findAllByOrderBySettlementDateDesc(pageable)
                .map(this::mapToResponse);
    }

    private CodSettlementBatchResponse mapToResponse(CodSettlementBatchEntity entity) {
        return new CodSettlementBatchResponse(
                entity.getId(),
                entity.getBatchReference(),
                entity.getTransportAgencyName(),
                entity.getSettlementDate(),
                entity.getTotalCollectedAmount(),
                entity.getRemittedAmount(),
                entity.getDiscrepancyAmount(),
                entity.getStatus(),
                entity.getPreparedBy(),
                entity.getApprovedBy(),
                entity.getNotes(),
                entity.getCreatedAt(),
                entity.getApprovedAt()
        );
    }

    private void saveOutboxEvent(String aggregateType, String aggregateId, String eventType, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            outboxEventRepository.save(new OutboxEventEntity(aggregateType, aggregateId, eventType, json));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox event for {} {}", aggregateType, aggregateId, e);
            throw new RuntimeException("Failed to persist outbox event", e);
        }
    }
}
