package com.dzfashion.commerce.repository;

import com.dzfashion.commerce.entity.OrderClaimHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderClaimHistoryRepository extends JpaRepository<OrderClaimHistoryEntity, UUID> {
    List<OrderClaimHistoryEntity> findByOrderIdOrderByCreatedAtDesc(UUID orderId);
}
