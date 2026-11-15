package com.dzfashion.inventory.repository;

import com.dzfashion.inventory.entity.LowStockAlertEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LowStockAlertRepository extends JpaRepository<LowStockAlertEntity, UUID> {

    List<LowStockAlertEntity> findByResolvedFalseOrderByAlertSentAtDesc();

    Page<LowStockAlertEntity> findByResolvedFalseOrderByAlertSentAtDesc(Pageable pageable);

    Optional<LowStockAlertEntity> findByVariantIdAndResolvedFalse(UUID variantId);

    boolean existsByVariantIdAndResolvedFalse(UUID variantId);
}
