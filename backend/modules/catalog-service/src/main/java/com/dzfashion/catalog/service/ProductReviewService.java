package com.dzfashion.catalog.service;

import com.dzfashion.catalog.client.PurchaseVerificationClient;
import com.dzfashion.catalog.dto.request.CreateProductReviewRequest;
import com.dzfashion.catalog.dto.request.ModerateReviewRequest;
import com.dzfashion.catalog.dto.response.PageResponse;
import com.dzfashion.catalog.dto.response.ProductReviewResponse;
import com.dzfashion.catalog.dto.response.ProductReviewSummaryResponse;
import com.dzfashion.catalog.entity.ProductEntity;
import com.dzfashion.catalog.entity.ProductReviewEntity;
import com.dzfashion.catalog.enums.ReviewStatus;
import com.dzfashion.catalog.exception.InvalidOperationException;
import com.dzfashion.catalog.exception.ResourceConflictException;
import com.dzfashion.catalog.exception.ResourceNotFoundException;
import com.dzfashion.catalog.repository.ProductRepository;
import com.dzfashion.catalog.repository.ProductReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class ProductReviewService {

    private static final Logger log = LoggerFactory.getLogger(ProductReviewService.class);

    private final ProductReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final PurchaseVerificationClient purchaseVerificationClient;

    public ProductReviewService(ProductReviewRepository reviewRepository,
                                ProductRepository productRepository,
                                PurchaseVerificationClient purchaseVerificationClient) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.purchaseVerificationClient = purchaseVerificationClient;
    }

    public ProductReviewResponse createReview(UUID productId, CreateProductReviewRequest request, UUID customerId) {
        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));

        if (reviewRepository.existsByOrderItemId(request.orderItemId())) {
            throw new ResourceConflictException("A review has already been submitted for order item: " + request.orderItemId());
        }

        PurchaseVerificationClient.PurchaseVerificationResult verificationResult =
                purchaseVerificationClient.verifyDeliveredPurchase(customerId, request.orderId(), request.orderItemId(), productId);

        if (!verificationResult.verified()) {
            throw new InvalidOperationException("Review creation rejected: only customers with a confirmed delivered order item can review. "
                    + (verificationResult.failureReason() != null ? verificationResult.failureReason() : ""));
        }

        ProductReviewEntity review = new ProductReviewEntity(
                product,
                customerId,
                request.orderId(),
                request.orderItemId(),
                request.rating(),
                request.title() != null ? request.title().trim() : null,
                request.comment().trim(),
                request.photoUrls()
        );

        ProductReviewEntity saved = reviewRepository.save(review);
        log.info("Customer {} submitted verified review {} for product {} (rating: {})",
                customerId, saved.getId(), productId, saved.getRating());

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductReviewResponse> getPublishedReviewsByProduct(UUID productId, Pageable pageable) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found with ID: " + productId);
        }

        Page<ProductReviewEntity> page = reviewRepository.findByProductIdAndStatus(productId, ReviewStatus.PUBLISHED, pageable);
        List<ProductReviewResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    @Transactional(readOnly = true)
    public ProductReviewSummaryResponse getProductReviewSummary(UUID productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found with ID: " + productId);
        }

        List<Object[]> stats = reviewRepository.getReviewStatsByProductIdAndStatus(productId, ReviewStatus.PUBLISHED);
        long totalReviews = 0;
        double averageRating = 0.0;

        if (stats != null && !stats.isEmpty() && stats.get(0) != null) {
            Object[] row = stats.get(0);
            if (row.length > 0 && row[0] instanceof Number countNum) {
                totalReviews = countNum.longValue();
            }
            if (row.length > 1 && row[1] instanceof Number avgNum) {
                double rawAvg = avgNum.doubleValue();
                averageRating = BigDecimal.valueOf(rawAvg)
                        .setScale(1, RoundingMode.HALF_UP)
                        .doubleValue();
            }
        }

        Map<Integer, Long> ratingCounts = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            ratingCounts.put(i, 0L);
        }

        List<Object[]> counts = reviewRepository.countRatingsByProductIdAndStatus(productId, ReviewStatus.PUBLISHED);
        if (counts != null) {
            for (Object[] countRow : counts) {
                if (countRow.length >= 2 && countRow[0] instanceof Number r && countRow[1] instanceof Number c) {
                    ratingCounts.put(r.intValue(), c.longValue());
                }
            }
        }

        return new ProductReviewSummaryResponse(productId, averageRating, totalReviews, ratingCounts);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductReviewResponse> getAllReviewsForAdmin(ReviewStatus statusFilter, Pageable pageable) {
        Page<ProductReviewEntity> page = statusFilter != null
                ? reviewRepository.findByStatus(statusFilter, pageable)
                : reviewRepository.findAll(pageable);

        List<ProductReviewResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    public ProductReviewResponse moderateReview(UUID reviewId, ModerateReviewRequest request, UUID ownerId) {
        ProductReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with ID: " + reviewId));

        review.setStatus(request.status());
        review.setModerationReason(request.reason().trim());
        review.setModeratedBy(ownerId);
        review.setModeratedAt(Instant.now());

        ProductReviewEntity saved = reviewRepository.save(review);
        log.info("Review {} status updated to {} by Owner {}. Reason: {}",
                reviewId, request.status(), ownerId, request.reason());

        return mapToResponse(saved);
    }

    private ProductReviewResponse mapToResponse(ProductReviewEntity entity) {
        return new ProductReviewResponse(
                entity.getId(),
                entity.getProduct().getId(),
                entity.getProduct().getName(),
                entity.getCustomerId(),
                entity.getOrderId(),
                entity.getOrderItemId(),
                entity.getRating(),
                entity.getTitle(),
                entity.getComment(),
                entity.getPhotoUrls(),
                entity.isVerifiedPurchase(),
                entity.getStatus(),
                entity.getModerationReason(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
