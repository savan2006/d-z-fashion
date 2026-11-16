package com.dzfashion.commerce.repository;

import com.dzfashion.commerce.entity.OrderEntity;
import com.dzfashion.commerce.enums.OrderStatus;
import com.dzfashion.commerce.enums.PaymentMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {
    Optional<OrderEntity> findByOrderNumber(String orderNumber);
    Optional<OrderEntity> findByIdempotencyKey(String idempotencyKey);
    Page<OrderEntity> findByCustomerIdOrderByCreatedAtDesc(UUID customerId, Pageable pageable);
    Page<OrderEntity> findByStatusInOrderByConfirmedAtAsc(Collection<OrderStatus> statuses, Pageable pageable);
    Page<OrderEntity> findAllByOrderByConfirmedAtDesc(Pageable pageable);

    long countByStatus(OrderStatus status);
    long countByAssignedManagerIdIsNullAndStatusIn(Collection<OrderStatus> statuses);
    long countByAssignedManagerIdIsNotNullAndStatusIn(Collection<OrderStatus> statuses);
    long countByPaymentMethod(PaymentMethod paymentMethod);

    @Query("SELECT COALESCE(SUM(o.subtotalBeforeDiscountAndTax), 0), COALESCE(SUM(o.promotionalDiscountAmount), 0), COALESCE(SUM(o.grandTotal), 0) FROM OrderEntity o WHERE o.status != com.dzfashion.commerce.enums.OrderStatus.CANCELLED")
    List<Object[]> getSalesFinancialSummary();

    @Query("SELECT o.paymentMethod, COUNT(o), COALESCE(SUM(o.grandTotal), 0) FROM OrderEntity o WHERE o.status != com.dzfashion.commerce.enums.OrderStatus.CANCELLED GROUP BY o.paymentMethod")
    List<Object[]> getSalesByPaymentMethod();
}
