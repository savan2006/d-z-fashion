package com.dzfashion.payment.repository;

import com.dzfashion.payment.entity.PaymentEntity;
import com.dzfashion.payment.enums.PaymentMethod;
import com.dzfashion.payment.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, UUID> {
    Optional<PaymentEntity> findByOrderId(UUID orderId);
    Optional<PaymentEntity> findByTransactionReference(String transactionReference);
    List<PaymentEntity> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);
    Page<PaymentEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentEntity p WHERE p.paymentMethod = :method AND p.status = :status")
    BigDecimal sumAmountByMethodAndStatus(@Param("method") PaymentMethod method, @Param("status") PaymentStatus status);

    @Query("SELECT COUNT(p) FROM PaymentEntity p WHERE p.status = :status")
    long countByStatus(@Param("status") PaymentStatus status);
}
