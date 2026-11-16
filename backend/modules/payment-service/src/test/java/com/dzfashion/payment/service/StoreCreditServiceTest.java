package com.dzfashion.payment.service;

import com.dzfashion.payment.dto.request.IssueStoreCreditRequest;
import com.dzfashion.payment.dto.request.RedeemStoreCreditRequest;
import com.dzfashion.payment.dto.response.StoreCreditBalanceResponse;
import com.dzfashion.payment.dto.response.StoreCreditEntryResponse;
import com.dzfashion.payment.entity.OutboxEventEntity;
import com.dzfashion.payment.entity.StoreCreditLedgerEntity;
import com.dzfashion.payment.enums.StoreCreditEntryType;
import com.dzfashion.payment.enums.StoreCreditReferenceType;
import com.dzfashion.payment.exception.InsufficientCreditException;
import com.dzfashion.payment.repository.OutboxEventRepository;
import com.dzfashion.payment.repository.StoreCreditLedgerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreCreditServiceTest {

    @Mock
    private StoreCreditLedgerRepository storeCreditLedgerRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private StoreCreditService service;

    @BeforeEach
    void setUp() {
        service = new StoreCreditService(storeCreditLedgerRepository, outboxEventRepository, objectMapper);
    }

    @Test
    @DisplayName("issueStoreCredit for new customer starts with balance equal to credited amount")
    void issueStoreCredit_NewCustomer_Success() {
        UUID customerId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        IssueStoreCreditRequest request = new IssueStoreCreditRequest(
                customerId,
                new BigDecimal("500.00"),
                StoreCreditReferenceType.MANUAL_ADJUSTMENT,
                "REF-001",
                "Loyalty reward issued by owner"
        );

        when(storeCreditLedgerRepository.findTopByCustomerIdOrderByCreatedAtDesc(customerId))
                .thenReturn(Optional.empty());
        when(storeCreditLedgerRepository.save(any(StoreCreditLedgerEntity.class)))
                .thenAnswer(inv -> {
                    StoreCreditLedgerEntity e = inv.getArgument(0);
                    e.setId(UUID.randomUUID());
                    return e;
                });

        StoreCreditEntryResponse response = service.issueStoreCredit(request, ownerId);

        assertThat(response.amount()).isEqualByComparingTo("500.00");
        assertThat(response.balanceAfter()).isEqualByComparingTo("500.00");
        assertThat(response.entryType()).isEqualTo(StoreCreditEntryType.CREDIT_ISSUED);

        ArgumentCaptor<OutboxEventEntity> outboxCaptor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().getEventType()).isEqualTo("STORE_CREDIT_ISSUED");
    }

    @Test
    @DisplayName("issueStoreCredit accumulates on existing balance")
    void issueStoreCredit_AccumulatesBalance() {
        UUID customerId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        StoreCreditLedgerEntity previous = new StoreCreditLedgerEntity(
                customerId,
                new BigDecimal("300.00"),
                new BigDecimal("300.00"),
                StoreCreditEntryType.CREDIT_ISSUED,
                StoreCreditReferenceType.RETURN_REFUND,
                "REF-RET-1",
                "Initial refund"
        );

        when(storeCreditLedgerRepository.findTopByCustomerIdOrderByCreatedAtDesc(customerId))
                .thenReturn(Optional.of(previous));
        when(storeCreditLedgerRepository.save(any(StoreCreditLedgerEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        IssueStoreCreditRequest request = new IssueStoreCreditRequest(
                customerId,
                new BigDecimal("250.00"),
                StoreCreditReferenceType.MANUAL_ADJUSTMENT,
                "ADJ-001",
                "Goodwill credit"
        );

        StoreCreditEntryResponse response = service.issueStoreCredit(request, ownerId);

        assertThat(response.balanceAfter()).isEqualByComparingTo("550.00");
    }

    @Test
    @DisplayName("redeemStoreCredit deducts from balance when sufficient funds exist")
    void redeemStoreCredit_SufficientBalance_Success() {
        UUID customerId = UUID.randomUUID();
        StoreCreditLedgerEntity previous = new StoreCreditLedgerEntity(
                customerId,
                new BigDecimal("1000.00"),
                new BigDecimal("1000.00"),
                StoreCreditEntryType.CREDIT_ISSUED,
                StoreCreditReferenceType.MANUAL_ADJUSTMENT,
                "DEP-1",
                "Deposit"
        );

        when(storeCreditLedgerRepository.findTopByCustomerIdOrderByCreatedAtDesc(customerId))
                .thenReturn(Optional.of(previous));
        when(storeCreditLedgerRepository.save(any(StoreCreditLedgerEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        RedeemStoreCreditRequest request = new RedeemStoreCreditRequest(customerId, new BigDecimal("400.00"), "ORD-20261116-01");
        StoreCreditEntryResponse response = service.redeemStoreCredit(request);

        assertThat(response.entryType()).isEqualTo(StoreCreditEntryType.CREDIT_USED);
        assertThat(response.balanceAfter()).isEqualByComparingTo("600.00");

        verify(outboxEventRepository).save(any(OutboxEventEntity.class));
    }

    @Test
    @DisplayName("redeemStoreCredit throws InsufficientCreditException when requested amount exceeds balance")
    void redeemStoreCredit_InsufficientBalance_ThrowsException() {
        UUID customerId = UUID.randomUUID();
        StoreCreditLedgerEntity previous = new StoreCreditLedgerEntity(
                customerId,
                new BigDecimal("150.00"),
                new BigDecimal("150.00"),
                StoreCreditEntryType.CREDIT_ISSUED,
                StoreCreditReferenceType.MANUAL_ADJUSTMENT,
                "DEP-1",
                "Deposit"
        );

        when(storeCreditLedgerRepository.findTopByCustomerIdOrderByCreatedAtDesc(customerId))
                .thenReturn(Optional.of(previous));

        RedeemStoreCreditRequest request = new RedeemStoreCreditRequest(customerId, new BigDecimal("300.00"), "ORD-20261116-02");
        assertThatThrownBy(() -> service.redeemStoreCredit(request))
                .isInstanceOf(InsufficientCreditException.class)
                .hasMessageContaining("Available balance ₹150.00 is insufficient for requested tender ₹300.00");
    }

    @Test
    @DisplayName("redeemStoreCredit with zero balance throws InsufficientCreditException")
    void redeemStoreCredit_ZeroBalance_ThrowsException() {
        UUID customerId = UUID.randomUUID();
        when(storeCreditLedgerRepository.findTopByCustomerIdOrderByCreatedAtDesc(customerId))
                .thenReturn(Optional.empty());

        RedeemStoreCreditRequest request = new RedeemStoreCreditRequest(customerId, new BigDecimal("50.00"), "ORD-20261116-03");
        assertThatThrownBy(() -> service.redeemStoreCredit(request))
                .isInstanceOf(InsufficientCreditException.class);
    }

    @Test
    @DisplayName("getBalance returns zero for customer with no prior ledger entries")
    void getBalance_NoEntries_ReturnsZero() {
        UUID customerId = UUID.randomUUID();
        when(storeCreditLedgerRepository.findTopByCustomerIdOrderByCreatedAtDesc(customerId))
                .thenReturn(Optional.empty());

        StoreCreditBalanceResponse balance = service.getBalance(customerId);
        assertThat(balance.currentBalance()).isEqualByComparingTo("0.00");
    }
}
