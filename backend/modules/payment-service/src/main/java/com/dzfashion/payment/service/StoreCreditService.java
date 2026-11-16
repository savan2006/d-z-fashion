package com.dzfashion.payment.service;

import com.dzfashion.payment.dto.request.IssueStoreCreditRequest;
import com.dzfashion.payment.dto.request.RedeemStoreCreditRequest;
import com.dzfashion.payment.dto.response.PageResponse;
import com.dzfashion.payment.dto.response.StoreCreditBalanceResponse;
import com.dzfashion.payment.dto.response.StoreCreditEntryResponse;
import com.dzfashion.payment.entity.OutboxEventEntity;
import com.dzfashion.payment.entity.StoreCreditLedgerEntity;
import com.dzfashion.payment.enums.StoreCreditEntryType;
import com.dzfashion.payment.enums.StoreCreditReferenceType;
import com.dzfashion.payment.exception.InsufficientCreditException;
import com.dzfashion.payment.exception.InvalidOperationException;
import com.dzfashion.payment.repository.OutboxEventRepository;
import com.dzfashion.payment.repository.StoreCreditLedgerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
public class StoreCreditService {

    private final StoreCreditLedgerRepository ledgerRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public StoreCreditService(StoreCreditLedgerRepository ledgerRepository,
                              OutboxEventRepository outboxEventRepository,
                              ObjectMapper objectMapper) {
        this.ledgerRepository = ledgerRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public BigDecimal getCurrentBalance(UUID customerId) {
        return ledgerRepository.findTopByCustomerIdOrderByCreatedAtDesc(customerId)
                .map(StoreCreditLedgerEntity::getBalanceAfter)
                .orElse(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
    }

    @Transactional(readOnly = true)
    public StoreCreditBalanceResponse getBalanceResponse(UUID customerId) {
        return new StoreCreditBalanceResponse(customerId, getCurrentBalance(customerId));
    }

    @Transactional(readOnly = true)
    public StoreCreditBalanceResponse getBalance(UUID customerId) {
        return getBalanceResponse(customerId);
    }

    @Transactional
    public synchronized StoreCreditEntryResponse issueCredit(IssueStoreCreditRequest request) {
        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOperationException("Store credit issue amount must be strictly greater than zero");
        }

        BigDecimal currentBalance = getCurrentBalance(request.customerId());
        BigDecimal newBalance = currentBalance.add(request.amount()).setScale(2, RoundingMode.HALF_UP);

        StoreCreditLedgerEntity entry = new StoreCreditLedgerEntity(
                request.customerId(),
                request.amount().setScale(2, RoundingMode.HALF_UP),
                newBalance,
                StoreCreditEntryType.CREDIT_ISSUED,
                request.referenceType(),
                request.referenceId(),
                request.notes()
        );

        StoreCreditLedgerEntity saved = ledgerRepository.save(entry);
        String eventId = saved.getId() != null ? saved.getId().toString() : request.customerId().toString();
        saveOutboxEvent("STORE_CREDIT", eventId, "STORE_CREDIT_ISSUED", saved);
        return mapToResponse(saved);
    }

    @Transactional
    public synchronized StoreCreditEntryResponse redeemCredit(RedeemStoreCreditRequest request) {
        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOperationException("Store credit redemption amount must be strictly greater than zero");
        }

        BigDecimal currentBalance = getCurrentBalance(request.customerId());
        if (currentBalance.compareTo(request.amount()) < 0) {
            throw new InsufficientCreditException("Available balance ₹" + currentBalance + " is insufficient for requested tender ₹" + request.amount());
        }

        BigDecimal newBalance = currentBalance.subtract(request.amount()).setScale(2, RoundingMode.HALF_UP);

        StoreCreditLedgerEntity entry = new StoreCreditLedgerEntity(
                request.customerId(),
                request.amount().negate().setScale(2, RoundingMode.HALF_UP), // Negative amount for debit in ledger
                newBalance,
                StoreCreditEntryType.CREDIT_USED,
                StoreCreditReferenceType.ORDER,
                request.orderNumber(),
                "Store credit tender used for order " + request.orderNumber()
        );

        StoreCreditLedgerEntity saved = ledgerRepository.save(entry);
        String eventId = saved.getId() != null ? saved.getId().toString() : request.customerId().toString();
        saveOutboxEvent("STORE_CREDIT", eventId, "STORE_CREDIT_REDEEMED", saved);
        return mapToResponse(saved);
    }

    @Transactional
    public StoreCreditEntryResponse issueStoreCredit(IssueStoreCreditRequest request, UUID actorId) {
        return issueCredit(request);
    }

    @Transactional
    public StoreCreditEntryResponse issueStoreCredit(IssueStoreCreditRequest request) {
        return issueCredit(request);
    }

    @Transactional
    public StoreCreditEntryResponse redeemStoreCredit(RedeemStoreCreditRequest request) {
        return redeemCredit(request);
    }

    @Transactional(readOnly = true)
    public Page<StoreCreditEntryResponse> getCustomerLedger(UUID customerId, Pageable pageable) {
        return ledgerRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable)
                .map(this::mapToResponse);
    }

    private StoreCreditEntryResponse mapToResponse(StoreCreditLedgerEntity entity) {
        return new StoreCreditEntryResponse(
                entity.getId(),
                entity.getCustomerId(),
                entity.getAmount(),
                entity.getBalanceAfter(),
                entity.getEntryType(),
                entity.getReferenceType(),
                entity.getReferenceId(),
                entity.getNotes(),
                entity.getCreatedAt()
        );
    }

    private void saveOutboxEvent(String type, String id, String eventType, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            outboxEventRepository.save(new OutboxEventEntity(type, id, eventType, json));
        } catch (Exception e) {
            // Outbox error handling
        }
    }
}
