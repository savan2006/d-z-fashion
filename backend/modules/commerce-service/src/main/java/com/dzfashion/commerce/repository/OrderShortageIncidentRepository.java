package com.dzfashion.commerce.repository;

import com.dzfashion.commerce.entity.OrderShortageIncidentEntity;
import com.dzfashion.commerce.enums.ShortageIncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderShortageIncidentRepository extends JpaRepository<OrderShortageIncidentEntity, UUID> {
    Optional<OrderShortageIncidentEntity> findByIncidentNumber(String incidentNumber);
    List<OrderShortageIncidentEntity> findByOrderId(UUID orderId);
    List<OrderShortageIncidentEntity> findByStatusOrderByCreatedAtDesc(ShortageIncidentStatus status);
    List<OrderShortageIncidentEntity> findAllByOrderByCreatedAtDesc();
    long countByStatus(ShortageIncidentStatus status);
}
