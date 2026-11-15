package com.dzfashion.catalog.repository;

import com.dzfashion.catalog.entity.ProductVariantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariantEntity, UUID> {

    Optional<ProductVariantEntity> findBySku(String sku);

    boolean existsBySku(String sku);

    boolean existsByProductIdAndSizeAndColour(UUID productId, String size, String colour);

    List<ProductVariantEntity> findByProductId(UUID productId);

    List<ProductVariantEntity> findByProductIdAndActiveTrue(UUID productId);
}
