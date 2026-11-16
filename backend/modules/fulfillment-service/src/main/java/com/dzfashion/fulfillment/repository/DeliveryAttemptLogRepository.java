package com.dzfashion.fulfillment.repository;

import com.dzfashion.fulfillment.entity.DeliveryAttemptLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DeliveryAttemptLogRepository extends JpaRepository<DeliveryAttemptLogEntity, UUID> {
    List<DeliveryAttemptLogEntity> findByShipmentIdOrderByAttemptNumberAsc(UUID shipmentId);
}
