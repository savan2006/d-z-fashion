package com.dzfashion.payment.repository;

import com.dzfashion.payment.entity.CodSettlementBatchEntity;
import com.dzfashion.payment.enums.CodSettlementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CodSettlementBatchRepository extends JpaRepository<CodSettlementBatchEntity, UUID> {
    Optional<CodSettlementBatchEntity> findByBatchReference(String batchReference);
    List<CodSettlementBatchEntity> findByStatusOrderBySettlementDateDesc(CodSettlementStatus status);
    Page<CodSettlementBatchEntity> findAllByOrderBySettlementDateDesc(Pageable pageable);
}
