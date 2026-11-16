package com.dzfashion.commerce.repository;

import com.dzfashion.commerce.entity.NotificationDeliveryEntity;
import com.dzfashion.commerce.enums.NotificationDeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationDeliveryRepository extends JpaRepository<NotificationDeliveryEntity, UUID> {

    List<NotificationDeliveryEntity> findByNotificationId(UUID notificationId);

    Page<NotificationDeliveryEntity> findByStatus(NotificationDeliveryStatus status, Pageable pageable);

    long countByStatus(NotificationDeliveryStatus status);
}
