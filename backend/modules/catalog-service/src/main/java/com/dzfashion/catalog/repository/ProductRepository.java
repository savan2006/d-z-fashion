package com.dzfashion.catalog.repository;

import com.dzfashion.catalog.entity.ProductEntity;
import com.dzfashion.catalog.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, UUID>, JpaSpecificationExecutor<ProductEntity> {

    Optional<ProductEntity> findBySlug(String slug);

    Optional<ProductEntity> findBySlugAndStatus(String slug, ProductStatus status);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    Page<ProductEntity> findByStatus(ProductStatus status, Pageable pageable);

    Page<ProductEntity> findByCategoryIdAndStatus(UUID categoryId, ProductStatus status, Pageable pageable);
}
