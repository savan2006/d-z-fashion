package com.dzfashion.commerce.service;

import com.dzfashion.commerce.client.EmailSenderClient;
import com.dzfashion.commerce.dto.response.NotificationResponse;
import com.dzfashion.commerce.dto.response.UnreadNotificationCountResponse;
import com.dzfashion.commerce.entity.NotificationDeliveryEntity;
import com.dzfashion.commerce.entity.NotificationEntity;
import com.dzfashion.commerce.enums.NotificationDeliveryStatus;
import com.dzfashion.commerce.enums.NotificationType;
import com.dzfashion.commerce.repository.NotificationDeliveryRepository;
import com.dzfashion.commerce.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationDeliveryRepository deliveryRepository;

    @Mock
    private EmailSenderClient emailSenderClient;

    @InjectMocks
    private NotificationService notificationService;

    private UUID recipientId;

    @BeforeEach
    void setUp() {
        recipientId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Create in-app notification persists notification with unread status")
    void testCreateInAppNotification() {
        when(notificationRepository.save(any(NotificationEntity.class))).thenAnswer(invocation -> {
            NotificationEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        NotificationResponse response = notificationService.createNotification(
                recipientId, null, NotificationType.ORDER_CONFIRMED,
                "Order Confirmed", "Your order ORD-001 has been placed.",
                "ORD-001", "Order"
        );

        assertThat(response).isNotNull();
        assertThat(response.type()).isEqualTo(NotificationType.ORDER_CONFIRMED);
        assertThat(response.title()).isEqualTo("Order Confirmed");
        assertThat(response.read()).isFalse();
    }

    @Test
    @DisplayName("Create notification with email dispatches email and records SENT delivery")
    void testCreateNotificationWithEmail_Success() {
        when(notificationRepository.save(any(NotificationEntity.class))).thenAnswer(invocation -> {
            NotificationEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        when(emailSenderClient.sendEmail(anyString(), anyString(), anyString()))
                .thenReturn(EmailSenderClient.EmailSendResult.success("MSG-12345"));

        notificationService.createNotificationWithEmail(
                recipientId, null, "customer@example.com",
                NotificationType.DISPATCHED, "Order Dispatched",
                "Your package is on its way", "ORD-001", "Order"
        );

        ArgumentCaptor<NotificationDeliveryEntity> captor = ArgumentCaptor.forClass(NotificationDeliveryEntity.class);
        verify(deliveryRepository).save(captor.capture());
        NotificationDeliveryEntity delivery = captor.getValue();

        assertThat(delivery.getStatus()).isEqualTo(NotificationDeliveryStatus.SENT);
        assertThat(delivery.getAttempts()).isEqualTo(1);
        assertThat(delivery.getRecipientAddress()).isEqualTo("customer@example.com");
    }

    @Test
    @DisplayName("Downstream email delivery failure is safely captured without failing the operation")
    void testCreateNotificationWithEmail_EmailFails_DoesNotThrow() {
        when(notificationRepository.save(any(NotificationEntity.class))).thenAnswer(invocation -> {
            NotificationEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        when(emailSenderClient.sendEmail(anyString(), anyString(), anyString()))
                .thenReturn(EmailSenderClient.EmailSendResult.failure("SMTP timeout"));

        NotificationResponse response = notificationService.createNotificationWithEmail(
                recipientId, null, "customer@example.com",
                NotificationType.ORDER_CONFIRMED, "Order Confirmed",
                "Your order ORD-001 has been confirmed", "ORD-001", "Order"
        );

        assertThat(response).isNotNull(); // In-app notification still succeeded!
        ArgumentCaptor<NotificationDeliveryEntity> captor = ArgumentCaptor.forClass(NotificationDeliveryEntity.class);
        verify(deliveryRepository).save(captor.capture());
        NotificationDeliveryEntity delivery = captor.getValue();

        assertThat(delivery.getStatus()).isEqualTo(NotificationDeliveryStatus.FAILED);
        assertThat(delivery.getErrorMessage()).contains("SMTP timeout");
    }

    @Test
    @DisplayName("Mark notification as read updates read flag and timestamp")
    void testMarkAsRead() {
        UUID notifId = UUID.randomUUID();
        NotificationEntity entity = new NotificationEntity(
                recipientId, null, NotificationType.DELIVERED,
                "Delivered", "Parcel delivered", "ORD-001", "Order"
        );
        entity.setId(notifId);

        when(notificationRepository.findById(notifId)).thenReturn(Optional.of(entity));
        when(notificationRepository.save(any(NotificationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse response = notificationService.markAsRead(notifId, recipientId);

        assertThat(response.read()).isTrue();
        assertThat(response.readAt()).isNotNull();
    }

    @Test
    @DisplayName("Get unread count returns unread notifications tally")
    void testUnreadCount() {
        when(notificationRepository.countByRecipientIdAndReadFalse(recipientId)).thenReturn(5L);

        UnreadNotificationCountResponse response = notificationService.getUnreadCount(recipientId);

        assertThat(response.unreadCount()).isEqualTo(5L);
    }
}
