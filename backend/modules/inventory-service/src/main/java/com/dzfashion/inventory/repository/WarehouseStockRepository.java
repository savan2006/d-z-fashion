package com.dzfashion.inventory.repository;

import com.dzfashion.inventory.entity.WarehouseStockEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WarehouseStockRepository extends JpaRepository<WarehouseStockEntity, UUID> {

    Optional<WarehouseStockEntity> findByVariantId(UUID variantId);

    Optional<WarehouseStockEntity> findBySku(String sku);

    boolean existsByVariantId(UUID variantId);

    boolean existsBySku(String sku);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ws FROM WarehouseStockEntity ws WHERE ws.variantId = :variantId")
    Optional<WarehouseStockEntity> findByVariantIdWithLock(@Param("variantId") UUID variantId);

    @Query("SELECT ws FROM WarehouseStockEntity ws WHERE (ws.physicalQty - ws.reservedQty) <= ws.lowStockThreshold ORDER BY (ws.physicalQty - ws.reservedQty) ASC")
    List<WarehouseStockEntity> findLowStockVariants();

    @Query("SELECT ws FROM WarehouseStockEntity ws WHERE (ws.physicalQty - ws.reservedQty) <= ws.lowStockThreshold ORDER BY (ws.physicalQty - ws.reservedQty) ASC")
    Page<WarehouseStockEntity> findLowStockVariants(Pageable pageable);
}
