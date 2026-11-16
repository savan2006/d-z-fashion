package com.dzfashion.commerce.service;

import com.dzfashion.commerce.entity.OutboxEventEntity;
import com.dzfashion.commerce.entity.ProcessedEventEntity;
import com.dzfashion.commerce.enums.NotificationType;
import com.dzfashion.commerce.repository.OutboxEventRepository;
import com.dzfashion.commerce.repository.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class OutboxRelayService {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayService.class);

    private final OutboxEventRepository outboxEventRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public OutboxRelayService(OutboxEventRepository outboxEventRepository,
                              ProcessedEventRepository processedEventRepository,
                              NotificationService notificationService,
                              AuditService auditService) {
        this.outboxEventRepository = outboxEventRepository;
        this.processedEventRepository = processedEventRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    public int processPendingEvents() {
        List<OutboxEventEntity> pendingEvents = outboxEventRepository.findByProcessedAtIsNullOrderByCreatedAtAsc();
        if (pendingEvents.isEmpty()) {
            return 0;
        }

        int processedCount = 0;
        for (OutboxEventEntity event : pendingEvents) {
            String eventId = event.getId() != null ? event.getId().toString() : UUID.randomUUID().toString();

            // Idempotency check: deduplicate if previously processed
            if (processedEventRepository.existsById(eventId)) {
                log.info("Duplicate event {} already processed. Skipping re-execution.", eventId);
                event.setProcessedAt(Instant.now());
                outboxEventRepository.save(event);
                continue;
            }

            try {
                handleEvent(event);

                // Record deduplication receipt
                processedEventRepository.save(new ProcessedEventEntity(eventId, event.getEventType()));
                event.setProcessedAt(Instant.now());
                outboxEventRepository.save(event);
                processedCount++;
            } catch (Exception ex) {
                log.error("Failed to relay outbox event {}: {}", eventId, ex.getMessage(), ex);
                // In resilient outbox design, don't crash whole loop; log failure
            }
        }

        return processedCount;
    }

    private void handleEvent(OutboxEventEntity event) {
        String eventType = event.getEventType();
        String aggregateId = event.getAggregateId();

        log.info("Relaying outbox event: type={}, aggregateType={}, aggregateId={}",
                eventType, event.getAggregateType(), aggregateId);

        switch (eventType) {
            case "ORDER_CONFIRMED" -> {
                // Internal staff alert for new order
                notificationService.createNotification(
                        UUID.fromString("00000000-0000-0000-0000-000000000000"),
                        "MANAGER",
                        NotificationType.ORDER_CONFIRMED,
                        "New Order Confirmed",
                        "Order " + aggregateId + " has been placed and is waiting for fulfillment claim.",
                        aggregateId,
                        "Order"
                );
            }
            case "ORDER_SHORTAGE_REPORTED" -> {
                notificationService.createNotification(
                        UUID.fromString("00000000-0000-0000-0000-000000000000"),
                        "OWNER",
                        NotificationType.LOW_STOCK_ALERT,
                        "Physical Stock Shortage Flagged",
                        "Shortage incident flagged for order " + aggregateId + ". Requires Owner customer contact.",
                        aggregateId,
                        "OrderShortage"
                );
            }
            case "ORDER_DISPATCHED" -> {
                notificationService.createNotification(
                        UUID.fromString("00000000-0000-0000-0000-000000000000"),
                        "MANAGER",
                        NotificationType.DISPATCHED,
                        "Order Dispatched",
                        "Order " + aggregateId + " dispatched to transport agency.",
                        aggregateId,
                        "Order"
                );
            }
            default -> {
                log.debug("Event type {} handled as general audit outbox message", eventType);
            }
        }
    }
}
