package com.dzfashion.catalog.controller;

import com.dzfashion.catalog.dto.request.CreateProductReviewRequest;
import com.dzfashion.catalog.dto.response.PageResponse;
import com.dzfashion.catalog.dto.response.ProductReviewResponse;
import com.dzfashion.catalog.dto.response.ProductReviewSummaryResponse;
import com.dzfashion.catalog.enums.ReviewStatus;
import com.dzfashion.catalog.enums.Role;
import com.dzfashion.catalog.security.JwtAuthenticationFilter;
import com.dzfashion.catalog.security.JwtTokenService;
import com.dzfashion.catalog.security.SecurityConfig;
import com.dzfashion.catalog.security.UserPrincipal;
import com.dzfashion.catalog.service.ProductReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductReviewController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class ProductReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ProductReviewService reviewService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @Test
    @DisplayName("Public GET /api/v1/products/{productId}/reviews returns 200 without authentication")
    void testGetProductReviews_Public() throws Exception {
        UUID productId = UUID.randomUUID();
        ProductReviewResponse response = new ProductReviewResponse(
                UUID.randomUUID(), productId, "Kurti", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                5, "Superb", "Amazing quality fabric", List.of(), true, ReviewStatus.PUBLISHED,
                null, Instant.now(), Instant.now()
        );

        when(reviewService.getPublishedReviewsByProduct(eq(productId), any()))
                .thenReturn(new PageResponse<>(List.of(response), 0, 10, 1, 1, true));

        mockMvc.perform(get("/api/v1/products/{productId}/reviews", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].rating").value(5))
                .andExpect(jsonPath("$.content[0].title").value("Superb"));
    }

    @Test
    @DisplayName("Public GET /api/v1/products/{productId}/reviews/summary returns rating statistics")
    void testGetProductReviewSummary_Public() throws Exception {
        UUID productId = UUID.randomUUID();
        ProductReviewSummaryResponse summary = new ProductReviewSummaryResponse(
                productId, 4.8, 12, Map.of(5, 10L, 4, 2L, 3, 0L, 2, 0L, 1, 0L)
        );

        when(reviewService.getProductReviewSummary(productId)).thenReturn(summary);

        mockMvc.perform(get("/api/v1/products/{productId}/reviews/summary", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").value(4.8))
                .andExpect(jsonPath("$.totalReviews").value(12));
    }

    @Test
    @DisplayName("POST /api/v1/products/{productId}/reviews requires authentication")
    void testCreateReview_Unauthenticated_Returns401() throws Exception {
        UUID productId = UUID.randomUUID();
        CreateProductReviewRequest request = new CreateProductReviewRequest(
                UUID.randomUUID(), UUID.randomUUID(), 5, "Nice", "Loved the product very much.", null
        );

        mockMvc.perform(post("/api/v1/products/{productId}/reviews", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/products/{productId}/reviews as CUSTOMER creates verified review")
    void testCreateReview_Customer_Returns201() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        CreateProductReviewRequest request = new CreateProductReviewRequest(
                UUID.randomUUID(), UUID.randomUUID(), 5, "Loved It", "Exquisite embroidery and premium stitch quality.", null
        );

        ProductReviewResponse response = new ProductReviewResponse(
                UUID.randomUUID(), productId, "Silk Suit", customerId, request.orderId(), request.orderItemId(),
                5, request.title(), request.comment(), List.of(), true, ReviewStatus.PUBLISHED,
                null, Instant.now(), Instant.now()
        );

        when(reviewService.createReview(eq(productId), any(), eq(customerId))).thenReturn(response);

        UserPrincipal principal = new UserPrincipal(customerId, "customer", "customer@test.com", Role.CUSTOMER);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        mockMvc.perform(post("/api/v1/products/{productId}/reviews", productId)
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.title").value("Loved It"));
    }
}
