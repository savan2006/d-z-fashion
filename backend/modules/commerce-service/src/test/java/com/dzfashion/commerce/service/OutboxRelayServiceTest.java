package com.dzfashion.commerce.service;

import com.dzfashion.commerce.entity.OutboxEventEntity;
import com.dzfashion.commerce.entity.ProcessedEventEntity;
import com.dzfashion.commerce.enums.NotificationType;
import com.dzfashion.commerce.repository.OutboxEventRepository;
import com.dzfashion.commerce.repository.ProcessedEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRelayServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private OutboxRelayService outboxRelayService;

    @Test
    @DisplayName("Pending outbox events are processed, dispatched to consumers, and marked as processed")
    void testProcessPendingEvents_Success() {
        UUID eventId = UUID.randomUUID();
        OutboxEventEntity event = new OutboxEventEntity("Order", "ORD-12345", "ORDER_CONFIRMED", "{\"orderId\":\"ORD-12345\"}");
        event.setId(eventId);

        when(outboxEventRepository.findByProcessedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(event));
        when(processedEventRepository.existsById(eventId.toString())).thenReturn(false);

        int count = outboxRelayService.processPendingEvents();

        assertThat(count).isEqualTo(1);
        assertThat(event.getProcessedAt()).isNotNull();
        verify(notificationService).createNotification(
                any(), eq("MANAGER"), eq(NotificationType.ORDER_CONFIRMED), anyString(), anyString(), eq("ORD-12345"), eq("Order")
        );
        verify(processedEventRepository).save(any(ProcessedEventEntity.class));
        verify(outboxEventRepository).save(event);
    }

    @Test
    @DisplayName("Duplicate event with previously recorded event ID is skipped idempotently without re-dispatching")
    void testProcessPendingEvents_IdempotentDeduplication() {
        UUID eventId = UUID.randomUUID();
        OutboxEventEntity duplicateEvent = new OutboxEventEntity("Order", "ORD-12345", "ORDER_CONFIRMED", "{\"orderId\":\"ORD-12345\"}");
        duplicateEvent.setId(eventId);

        when(outboxEventRepository.findByProcessedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(duplicateEvent));
        when(processedEventRepository.existsById(eventId.toString())).thenReturn(true); // Already processed!

        int count = outboxRelayService.processPendingEvents();

        assertThat(count).isEqualTo(0);
        assertThat(duplicateEvent.getProcessedAt()).isNotNull();
        verify(notificationService, never()).createNotification(any(), any(), any(), any(), any(), any(), any());
        verify(processedEventRepository, never()).save(any(ProcessedEventEntity.class));
        verify(outboxEventRepository).save(duplicateEvent);
    }
}
