package com.dzfashion.catalog.repository;

import com.dzfashion.catalog.entity.ProductReviewEntity;
import com.dzfashion.catalog.enums.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReviewEntity, UUID> {

    boolean existsByOrderItemId(UUID orderItemId);

    Optional<ProductReviewEntity> findByOrderItemId(UUID orderItemId);

    Page<ProductReviewEntity> findByProductIdAndStatus(UUID productId, ReviewStatus status, Pageable pageable);

    Page<ProductReviewEntity> findByProductId(UUID productId, Pageable pageable);

    Page<ProductReviewEntity> findByCustomerId(UUID customerId, Pageable pageable);

    Page<ProductReviewEntity> findByStatus(ReviewStatus status, Pageable pageable);

    @Query("SELECT r.rating, COUNT(r) FROM ProductReviewEntity r WHERE r.product.id = :productId AND r.status = :status GROUP BY r.rating")
    List<Object[]> countRatingsByProductIdAndStatus(@Param("productId") UUID productId, @Param("status") ReviewStatus status);

    @Query("SELECT COUNT(r), COALESCE(AVG(r.rating), 0.0) FROM ProductReviewEntity r WHERE r.product.id = :productId AND r.status = :status")
    List<Object[]> getReviewStatsByProductIdAndStatus(@Param("productId") UUID productId, @Param("status") ReviewStatus status);
}
