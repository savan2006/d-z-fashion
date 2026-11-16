package com.dzfashion.payment.service;

import com.dzfashion.payment.dto.request.RecordCodCollectedRequest;
import com.dzfashion.payment.dto.request.RecordCodPendingRequest;
import com.dzfashion.payment.dto.request.ReversePaymentRequest;
import com.dzfashion.payment.dto.request.SimulatePaymentRequest;
import com.dzfashion.payment.dto.response.PaymentResponse;
import com.dzfashion.payment.entity.OutboxEventEntity;
import com.dzfashion.payment.entity.PaymentEntity;
import com.dzfashion.payment.enums.PaymentMethod;
import com.dzfashion.payment.enums.PaymentStatus;
import com.dzfashion.payment.exception.InvalidOperationException;
import com.dzfashion.payment.exception.ResourceNotFoundException;
import com.dzfashion.payment.repository.OutboxEventRepository;
import com.dzfashion.payment.repository.PaymentRepository;
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
class PaymentRecordServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private PaymentRecordService service;

    @BeforeEach
    void setUp() {
        service = new PaymentRecordService(paymentRepository, outboxEventRepository, objectMapper);
    }

    @Test
    @DisplayName("simulateOnlinePayment records virtual payment clearly marked isSimulated=true with no sensitive card details")
    void simulateOnlinePayment_Success() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        SimulatePaymentRequest request = new SimulatePaymentRequest(orderId, customerId, new BigDecimal("2499.00"));

        when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(invocation -> {
            PaymentEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        PaymentResponse response = service.simulateOnlinePayment(request);

        assertThat(response).isNotNull();
        assertThat(response.simulated()).isTrue();
        assertThat(response.method()).isEqualTo(PaymentMethod.SIMULATED_ONLINE);
        assertThat(response.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.amount()).isEqualByComparingTo("2499.00");
        assertThat(response.transactionReference()).startsWith("SIM-");

        ArgumentCaptor<OutboxEventEntity> outboxCaptor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().getEventType()).isEqualTo("PAYMENT_SIMULATED_SUCCESS");
    }

    @Test
    @DisplayName("recordCodPending creates pending COD payment marked isSimulated=false")
    void recordCodPending_Success() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        RecordCodPendingRequest request = new RecordCodPendingRequest(orderId, customerId, new BigDecimal("1850.00"), "395002");

        when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(invocation -> {
            PaymentEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        PaymentResponse response = service.recordCodPending(request);

        assertThat(response.simulated()).isFalse();
        assertThat(response.method()).isEqualTo(PaymentMethod.COD);
        assertThat(response.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.amount()).isEqualByComparingTo("1850.00");
        assertThat(response.transactionReference()).startsWith("COD-");
    }

    @Test
    @DisplayName("recordCodCollected transitions COD payment from PENDING to SUCCESS")
    void recordCodCollected_Success() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        PaymentEntity existing = new PaymentEntity(orderId, customerId, new BigDecimal("1500.00"),
                PaymentMethod.COD, PaymentStatus.PENDING, "COD-20261116-ABCDEF", false);
        existing.setId(UUID.randomUUID());

        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(existing));
        when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RecordCodCollectedRequest request = new RecordCodCollectedRequest(orderId, new BigDecimal("1500.00"));
        PaymentResponse response = service.recordCodCollected(request);

        assertThat(response.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.collectedAt()).isNotNull();

        verify(outboxEventRepository).save(any(OutboxEventEntity.class));
    }

    @Test
    @DisplayName("recordCodCollected on already collected payment throws InvalidOperationException")
    void recordCodCollected_AlreadySuccess_ThrowsException() {
        UUID orderId = UUID.randomUUID();
        PaymentEntity existing = new PaymentEntity(orderId, UUID.randomUUID(), new BigDecimal("1500.00"),
                PaymentMethod.COD, PaymentStatus.SUCCESS, "COD-20261116-ABCDEF", false);

        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(existing));

        RecordCodCollectedRequest request = new RecordCodCollectedRequest(orderId, new BigDecimal("1500.00"));
        assertThatThrownBy(() -> service.recordCodCollected(request))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Payment is already SUCCESS");
    }

    @Test
    @DisplayName("reversePayment transitions successful payment to REVERSED")
    void reversePayment_Success() {
        UUID orderId = UUID.randomUUID();
        PaymentEntity existing = new PaymentEntity(orderId, UUID.randomUUID(), new BigDecimal("2999.00"),
                PaymentMethod.SIMULATED_ONLINE, PaymentStatus.SUCCESS, "SIM-12345", true);
        existing.setId(UUID.randomUUID());

        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(existing));
        when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReversePaymentRequest request = new ReversePaymentRequest(orderId, "Customer return accepted by owner");
        PaymentResponse response = service.reversePayment(request);

        assertThat(response.status()).isEqualTo(PaymentStatus.REVERSED);
        verify(outboxEventRepository).save(any(OutboxEventEntity.class));
    }

    @Test
    @DisplayName("reversePayment on pending payment throws InvalidOperationException")
    void reversePayment_NotSuccess_ThrowsException() {
        UUID orderId = UUID.randomUUID();
        PaymentEntity existing = new PaymentEntity(orderId, UUID.randomUUID(), new BigDecimal("2999.00"),
                PaymentMethod.COD, PaymentStatus.PENDING, "COD-12345", false);

        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(existing));

        ReversePaymentRequest request = new ReversePaymentRequest(orderId, "Cancellation");
        assertThatThrownBy(() -> service.reversePayment(request))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Cannot reverse payment in status PENDING");
    }

    @Test
    @DisplayName("getPaymentByOrderId throws ResourceNotFoundException if absent")
    void getPaymentByOrderId_NotFound_ThrowsException() {
        UUID orderId = UUID.randomUUID();
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPaymentByOrderId(orderId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
