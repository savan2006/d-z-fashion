package com.dzfashion.catalog.repository;

import com.dzfashion.catalog.entity.MediaAssetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MediaAssetRepository extends JpaRepository<MediaAssetEntity, UUID> {

    List<MediaAssetEntity> findByProductIdOrderByDisplayOrderAsc(UUID productId);

    List<MediaAssetEntity> findByVariantId(UUID variantId);
}
