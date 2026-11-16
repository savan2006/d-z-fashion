package com.dzfashion.payment.repository;

import com.dzfashion.payment.entity.StoreCreditLedgerEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreCreditLedgerRepository extends JpaRepository<StoreCreditLedgerEntity, UUID> {
    Optional<StoreCreditLedgerEntity> findTopByCustomerIdOrderByCreatedAtDesc(UUID customerId);
    List<StoreCreditLedgerEntity> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);
    Page<StoreCreditLedgerEntity> findByCustomerIdOrderByCreatedAtDesc(UUID customerId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM StoreCreditLedgerEntity e")
    BigDecimal calculateTotalOutstandingCredit();
}
