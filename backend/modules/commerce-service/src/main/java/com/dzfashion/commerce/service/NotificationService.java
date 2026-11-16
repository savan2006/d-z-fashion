package com.dzfashion.commerce.service;

import com.dzfashion.commerce.client.EmailSenderClient;
import com.dzfashion.commerce.dto.response.NotificationResponse;
import com.dzfashion.commerce.dto.response.PageResponse;
import com.dzfashion.commerce.dto.response.UnreadNotificationCountResponse;
import com.dzfashion.commerce.entity.NotificationDeliveryEntity;
import com.dzfashion.commerce.entity.NotificationEntity;
import com.dzfashion.commerce.enums.NotificationDeliveryStatus;
import com.dzfashion.commerce.enums.NotificationType;
import com.dzfashion.commerce.exception.ResourceNotFoundException;
import com.dzfashion.commerce.repository.NotificationDeliveryRepository;
import com.dzfashion.commerce.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final EmailSenderClient emailSenderClient;

    public NotificationService(NotificationRepository notificationRepository,
                               NotificationDeliveryRepository deliveryRepository,
                               EmailSenderClient emailSenderClient) {
        this.notificationRepository = notificationRepository;
        this.deliveryRepository = deliveryRepository;
        this.emailSenderClient = emailSenderClient;
    }

    public NotificationResponse createNotification(UUID recipientId, String recipientRole, NotificationType type,
                                                  String title, String message, String referenceId, String referenceType) {
        NotificationEntity notification = new NotificationEntity(
                recipientId,
                recipientRole,
                type,
                title,
                message,
                referenceId,
                referenceType
        );

        NotificationEntity saved = notificationRepository.save(notification);
        log.info("Created in-app notification {} of type {} for recipient {} (role: {})",
                saved.getId(), type, recipientId, recipientRole);

        return mapToResponse(saved);
    }

    public NotificationResponse createNotificationWithEmail(UUID recipientId, String recipientRole, String recipientEmail,
                                                           NotificationType type, String title, String message,
                                                           String referenceId, String referenceType) {
        NotificationEntity notification = new NotificationEntity(
                recipientId,
                recipientRole,
                type,
                title,
                message,
                referenceId,
                referenceType
        );
        NotificationEntity saved = notificationRepository.save(notification);

        if (recipientEmail != null && !recipientEmail.isBlank()) {
            NotificationDeliveryEntity delivery = new NotificationDeliveryEntity(
                    saved.getId(),
                    "EMAIL",
                    recipientEmail,
                    title
            );

            try {
                EmailSenderClient.EmailSendResult result = emailSenderClient.sendEmail(recipientEmail, title, message);
                if (result.success()) {
                    delivery.recordSuccess();
                } else {
                    delivery.recordFailure(result.errorMessage());
                    log.warn("Downstream email send failed for notification {}: {}", saved.getId(), result.errorMessage());
                }
            } catch (Exception ex) {
                delivery.recordFailure(ex.getMessage());
                log.error("Unexpected error dispatching email for notification {}: {}", saved.getId(), ex.getMessage());
            }

            deliveryRepository.save(delivery);
        }

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getNotificationsForRecipient(UUID recipientId, Pageable pageable) {
        Page<NotificationEntity> page = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId, pageable);
        List<NotificationResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getNotificationsForRole(String role, Pageable pageable) {
        Page<NotificationEntity> page = notificationRepository.findByRecipientRoleOrderByCreatedAtDesc(role, pageable);
        List<NotificationResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    public NotificationResponse markAsRead(UUID notificationId, UUID recipientId) {
        NotificationEntity notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: " + notificationId));

        if (!notification.getRecipientId().equals(recipientId)) {
            throw new ResourceNotFoundException("Notification not found with ID: " + notificationId);
        }

        notification.markAsRead();
        NotificationEntity saved = notificationRepository.save(notification);
        return mapToResponse(saved);
    }

    public void markAllAsRead(UUID recipientId) {
        notificationRepository.markAllAsReadForRecipient(recipientId, Instant.now());
    }

    @Transactional(readOnly = true)
    public UnreadNotificationCountResponse getUnreadCount(UUID recipientId) {
        long count = notificationRepository.countByRecipientIdAndReadFalse(recipientId);
        return new UnreadNotificationCountResponse(count);
    }

    @Transactional(readOnly = true)
    public UnreadNotificationCountResponse getUnreadCountForRole(String role) {
        long count = notificationRepository.countByRecipientRoleAndReadFalse(role);
        return new UnreadNotificationCountResponse(count);
    }

    public int retryFailedDeliveries(Pageable pageable) {
        Page<NotificationDeliveryEntity> failedPage = deliveryRepository.findByStatus(NotificationDeliveryStatus.FAILED, pageable);
        int retriedCount = 0;

        for (NotificationDeliveryEntity delivery : failedPage.getContent()) {
            try {
                EmailSenderClient.EmailSendResult result = emailSenderClient.sendEmail(
                        delivery.getRecipientAddress(),
                        delivery.getSubject(),
                        "Retry notification content"
                );
                if (result.success()) {
                    delivery.recordSuccess();
                    retriedCount++;
                } else {
                    delivery.recordFailure(result.errorMessage());
                }
                deliveryRepository.save(delivery);
            } catch (Exception ex) {
                delivery.recordFailure(ex.getMessage());
                deliveryRepository.save(delivery);
            }
        }

        return retriedCount;
    }

    private NotificationResponse mapToResponse(NotificationEntity entity) {
        return new NotificationResponse(
                entity.getId(),
                entity.getRecipientId(),
                entity.getRecipientRole(),
                entity.getType(),
                entity.getTitle(),
                entity.getMessage(),
                entity.getReferenceId(),
                entity.getReferenceType(),
                entity.isRead(),
                entity.getReadAt(),
                entity.getCreatedAt()
        );
    }
}
