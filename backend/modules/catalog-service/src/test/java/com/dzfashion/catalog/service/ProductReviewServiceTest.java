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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductReviewServiceTest {

    @Mock
    private ProductReviewRepository reviewRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private PurchaseVerificationClient purchaseVerificationClient;

    @InjectMocks
    private ProductReviewService reviewService;

    private UUID productId;
    private UUID customerId;
    private UUID orderId;
    private UUID orderItemId;
    private ProductEntity productEntity;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        orderItemId = UUID.randomUUID();

        productEntity = new ProductEntity();
        productEntity.setId(productId);
        productEntity.setName("Anarkali Suit Set");
    }

    @Test
    @DisplayName("Verified purchaser submits a review with rating and photos; auto-published")
    void testCreateReview_VerifiedPurchaser_Success() {
        CreateProductReviewRequest request = new CreateProductReviewRequest(
                orderId, orderItemId, 5, "Exquisite Quality",
                "The fabric feels luxurious and the finishing is top-notch.",
                List.of("https://res.cloudinary.com/dzfashion/review1.jpg")
        );

        when(productRepository.findById(productId)).thenReturn(Optional.of(productEntity));
        when(reviewRepository.existsByOrderItemId(orderItemId)).thenReturn(false);
        when(purchaseVerificationClient.verifyDeliveredPurchase(customerId, orderId, orderItemId, productId))
                .thenReturn(PurchaseVerificationClient.PurchaseVerificationResult.success());

        when(reviewRepository.save(any(ProductReviewEntity.class))).thenAnswer(invocation -> {
            ProductReviewEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        ProductReviewResponse response = reviewService.createReview(productId, request, customerId);

        assertThat(response).isNotNull();
        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.title()).isEqualTo("Exquisite Quality");
        assertThat(response.status()).isEqualTo(ReviewStatus.PUBLISHED);
        assertThat(response.verifiedPurchase()).isTrue();
        assertThat(response.photoUrls()).hasSize(1);
        verify(reviewRepository).save(any(ProductReviewEntity.class));
    }

    @Test
    @DisplayName("Review submission fails if order item has already been reviewed")
    void testCreateReview_DuplicateOrderItem_ThrowsConflict() {
        CreateProductReviewRequest request = new CreateProductReviewRequest(
                orderId, orderItemId, 4, "Great fit",
                "Loved the embroidery work.", null
        );

        when(productRepository.findById(productId)).thenReturn(Optional.of(productEntity));
        when(reviewRepository.existsByOrderItemId(orderItemId)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.createReview(productId, request, customerId))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("A review has already been submitted");
    }

    @Test
    @DisplayName("Review submission fails if customer purchase has not been delivered")
    void testCreateReview_UnverifiedPurchase_ThrowsInvalidOperation() {
        CreateProductReviewRequest request = new CreateProductReviewRequest(
                orderId, orderItemId, 5, "Amazing",
                "Really looking forward to wearing it.", null
        );

        when(productRepository.findById(productId)).thenReturn(Optional.of(productEntity));
        when(reviewRepository.existsByOrderItemId(orderItemId)).thenReturn(false);
        when(purchaseVerificationClient.verifyDeliveredPurchase(customerId, orderId, orderItemId, productId))
                .thenReturn(PurchaseVerificationClient.PurchaseVerificationResult.failure("Order has not been delivered"));

        assertThatThrownBy(() -> reviewService.createReview(productId, request, customerId))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("only customers with a confirmed delivered order item can review");
    }

    @Test
    @DisplayName("Review summary aggregates average rating and rating breakdown correctly")
    void testGetProductReviewSummary_CalculatesAverageAndCounts() {
        when(productRepository.existsById(productId)).thenReturn(true);

        // Stats: total = 4, avg = 4.5
        List<Object[]> statsList = java.util.Collections.singletonList(new Object[]{4L, 4.5});
        when(reviewRepository.getReviewStatsByProductIdAndStatus(productId, ReviewStatus.PUBLISHED))
                .thenReturn(statsList);

        // Counts: 5-star: 2, 4-star: 2
        List<Object[]> counts = List.of(
                new Object[]{5, 2L},
                new Object[]{4, 2L}
        );
        when(reviewRepository.countRatingsByProductIdAndStatus(productId, ReviewStatus.PUBLISHED))
                .thenReturn(counts);

        ProductReviewSummaryResponse summary = reviewService.getProductReviewSummary(productId);

        assertThat(summary.productId()).isEqualTo(productId);
        assertThat(summary.averageRating()).isEqualTo(4.5);
        assertThat(summary.totalReviews()).isEqualTo(4L);
        assertThat(summary.ratingCounts().get(5)).isEqualTo(2L);
        assertThat(summary.ratingCounts().get(4)).isEqualTo(2L);
        assertThat(summary.ratingCounts().get(3)).isEqualTo(0L);
    }

    @Test
    @DisplayName("Owner successfully moderates and hides a review with recorded reason")
    void testModerateReview_OwnerSuccess() {
        UUID reviewId = UUID.randomUUID();
        ProductReviewEntity review = new ProductReviewEntity(
                productEntity, customerId, orderId, orderItemId,
                1, "Spam", "Spam advertising external links", null
        );
        review.setId(reviewId);

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(ProductReviewEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UUID ownerId = UUID.randomUUID();
        ModerateReviewRequest request = new ModerateReviewRequest(ReviewStatus.HIDDEN, "Contains inappropriate advertising link");
        ProductReviewResponse response = reviewService.moderateReview(reviewId, request, ownerId);

        assertThat(response.status()).isEqualTo(ReviewStatus.HIDDEN);
        assertThat(response.moderationReason()).isEqualTo("Contains inappropriate advertising link");
    }

    @Test
    @DisplayName("Get published reviews returns paginated list")
    void testGetPublishedReviews_ReturnsPaginated() {
        when(productRepository.existsById(productId)).thenReturn(true);

        ProductReviewEntity review = new ProductReviewEntity(
                productEntity, customerId, orderId, orderItemId,
                5, "Splendid", "Loved the fit and fabric quality.", null
        );
        review.setId(UUID.randomUUID());

        Pageable pageable = PageRequest.of(0, 10);
        when(reviewRepository.findByProductIdAndStatus(productId, ReviewStatus.PUBLISHED, pageable))
                .thenReturn(new PageImpl<>(List.of(review), pageable, 1));

        PageResponse<ProductReviewResponse> response = reviewService.getPublishedReviewsByProduct(productId, pageable);

        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).title()).isEqualTo("Splendid");
    }
}
