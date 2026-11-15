package com.dzfashion.inventory.repository;

import com.dzfashion.inventory.entity.StockMovementEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovementEntity, UUID> {

    List<StockMovementEntity> findByVariantIdOrderByCreatedAtDesc(UUID variantId);

    Page<StockMovementEntity> findByVariantIdOrderByCreatedAtDesc(UUID variantId, Pageable pageable);

    Page<StockMovementEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
