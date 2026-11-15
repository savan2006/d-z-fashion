package com.dzfashion.commerce.repository;

import com.dzfashion.commerce.entity.OrderEntity;
import com.dzfashion.commerce.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {
    Optional<OrderEntity> findByOrderNumber(String orderNumber);
    Optional<OrderEntity> findByIdempotencyKey(String idempotencyKey);
    Page<OrderEntity> findByCustomerIdOrderByCreatedAtDesc(UUID customerId, Pageable pageable);
    Page<OrderEntity> findByStatusInOrderByConfirmedAtAsc(Collection<OrderStatus> statuses, Pageable pageable);
    Page<OrderEntity> findAllByOrderByConfirmedAtDesc(Pageable pageable);
}
