package com.dzfashion.commerce.repository;

import com.dzfashion.commerce.entity.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    Page<NotificationEntity> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId, Pageable pageable);

    Page<NotificationEntity> findByRecipientRoleOrderByCreatedAtDesc(String recipientRole, Pageable pageable);

    Page<NotificationEntity> findByRecipientIdOrRecipientRoleOrderByCreatedAtDesc(UUID recipientId, String recipientRole, Pageable pageable);

    long countByRecipientIdAndReadFalse(UUID recipientId);

    long countByRecipientRoleAndReadFalse(String recipientRole);

    @Modifying
    @Query("UPDATE NotificationEntity n SET n.read = true, n.readAt = :now WHERE n.recipientId = :recipientId AND n.read = false")
    int markAllAsReadForRecipient(@Param("recipientId") UUID recipientId, @Param("now") Instant now);
}
