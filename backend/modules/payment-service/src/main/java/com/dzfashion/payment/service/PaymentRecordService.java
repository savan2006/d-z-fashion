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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class PaymentRecordService {

    private final PaymentRepository paymentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public PaymentRecordService(PaymentRepository paymentRepository,
                                OutboxEventRepository outboxEventRepository,
                                ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PaymentResponse simulateOnlinePayment(SimulatePaymentRequest request) {
        String datePrefix = LocalDate.now(ZoneId.of("Asia/Kolkata")).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String txRef = "SIM-" + datePrefix + "-" + randomSuffix;

        PaymentEntity payment = new PaymentEntity(
                request.orderId(),
                request.customerId(),
                request.amount(),
                PaymentMethod.SIMULATED_ONLINE,
                PaymentStatus.SUCCESS,
                txRef,
                true // Clearly marked is_simulated = true
        );

        PaymentEntity saved = paymentRepository.save(payment);
        saveOutboxEvent("PAYMENT", saved.getId().toString(), "PAYMENT_SIMULATED_SUCCESS", saved);
        return mapToResponse(saved);
    }

    @Transactional
    public PaymentResponse recordCodPending(RecordCodPendingRequest request) {
        String datePrefix = LocalDate.now(ZoneId.of("Asia/Kolkata")).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String txRef = "COD-" + datePrefix + "-" + randomSuffix;

        PaymentEntity payment = new PaymentEntity(
                request.orderId(),
                request.customerId(),
                request.amount(),
                PaymentMethod.COD,
                PaymentStatus.PENDING,
                txRef,
                false
        );
        payment.setDeliveryPincode(request.deliveryPincode());

        PaymentEntity saved = paymentRepository.save(payment);
        return mapToResponse(saved);
    }

    @Transactional
    public PaymentResponse recordCodCollected(RecordCodCollectedRequest request) {
        return recordCodCollected(request, null);
    }

    @Transactional
    public PaymentResponse recordCodCollected(RecordCodCollectedRequest request, UUID collectorId) {
        PaymentEntity payment = paymentRepository.findByOrderId(request.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment record for order " + request.orderId() + " not found"));

        if (payment.getMethod() != PaymentMethod.COD) {
            throw new InvalidOperationException("Order " + request.orderId() + " is not a Cash on Delivery payment");
        }

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new InvalidOperationException("Payment is already SUCCESS for order " + request.orderId());
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setCollectedAt(java.time.Instant.now());
        payment.setCollectedBy(collectorId);

        PaymentEntity saved = paymentRepository.save(payment);
        saveOutboxEvent("PAYMENT", saved.getId().toString(), "COD_PAYMENT_COLLECTED", saved);
        return mapToResponse(saved);
    }

    @Transactional
    public PaymentResponse reversePayment(ReversePaymentRequest request) {
        return reversePayment(request, null);
    }

    @Transactional
    public PaymentResponse reversePayment(ReversePaymentRequest request, UUID actorId) {
        PaymentEntity payment = paymentRepository.findByOrderId(request.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment record for order " + request.orderId() + " not found"));

        if (payment.getStatus() == PaymentStatus.VOIDED || payment.getStatus() == PaymentStatus.REVERSED) {
            return mapToResponse(payment); // Idempotent
        }

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new InvalidOperationException("Cannot reverse payment in status " + payment.getStatus());
        }

        if (payment.getMethod() == PaymentMethod.SIMULATED_ONLINE) {
            payment.setStatus(PaymentStatus.REVERSED);
        } else {
            payment.setStatus(PaymentStatus.VOIDED);
        }

        PaymentEntity saved = paymentRepository.save(payment);
        saveOutboxEvent("PAYMENT", saved.getId().toString(), "PAYMENT_REVERSED", saved);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(UUID orderId) {
        PaymentEntity payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment record for order " + orderId + " not found"));
        return mapToResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByTransactionReference(String txRef) {
        PaymentEntity payment = paymentRepository.findByTransactionReference(txRef)
                .orElseThrow(() -> new ResourceNotFoundException("Payment record not found for transaction reference: " + txRef));
        return mapToResponse(payment);
    }

    public PaymentResponse mapToResponse(PaymentEntity entity) {
        return new PaymentResponse(
                entity.getId(),
                entity.getOrderId(),
                entity.getCustomerId(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getMethod(),
                entity.getStatus(),
                entity.getTransactionReference(),
                entity.isSimulated(),
                entity.getDeliveryPincode(),
                entity.getCollectedAt(),
                entity.getCollectedBy(),
                entity.getCreatedAt()
        );
    }

    private void saveOutboxEvent(String type, String id, String eventType, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            outboxEventRepository.save(new OutboxEventEntity(type, id, eventType, json));
        } catch (Exception e) {
            // Outbox serialization exception handling
        }
    }
}
