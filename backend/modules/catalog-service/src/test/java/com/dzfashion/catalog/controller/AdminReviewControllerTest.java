package com.dzfashion.catalog.controller;

import com.dzfashion.catalog.dto.request.ModerateReviewRequest;
import com.dzfashion.catalog.dto.response.PageResponse;
import com.dzfashion.catalog.dto.response.ProductReviewResponse;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminReviewController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AdminReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ProductReviewService reviewService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @Test
    @DisplayName("GET /api/v1/admin/reviews as MANAGER returns 200 OK")
    void testGetAllReviews_Manager_Returns200() throws Exception {
        when(reviewService.getAllReviewsForAdmin(any(), any()))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true));

        UserPrincipal principal = new UserPrincipal(UUID.randomUUID(), "manager", "manager@test.com", Role.MANAGER);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        mockMvc.perform(get("/api/v1/admin/reviews")
                        .with(authentication(auth)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/admin/reviews as CUSTOMER returns 403 Forbidden")
    void testGetAllReviews_Customer_Returns403() throws Exception {
        UserPrincipal principal = new UserPrincipal(UUID.randomUUID(), "customer", "customer@test.com", Role.CUSTOMER);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        mockMvc.perform(get("/api/v1/admin/reviews")
                        .with(authentication(auth)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/reviews/{id}/status as OWNER moderates review successfully")
    void testModerateReview_Owner_Returns200() throws Exception {
        UUID reviewId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        ModerateReviewRequest request = new ModerateReviewRequest(ReviewStatus.HIDDEN, "Inappropriate content");

        ProductReviewResponse response = new ProductReviewResponse(
                reviewId, UUID.randomUUID(), "Suit", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                1, "Bad", "Inappropriate words", List.of(), true, ReviewStatus.HIDDEN,
                "Inappropriate content", Instant.now(), Instant.now()
        );

        when(reviewService.moderateReview(eq(reviewId), any(), eq(ownerId))).thenReturn(response);

        UserPrincipal principal = new UserPrincipal(ownerId, "owner", "owner@test.com", Role.OWNER);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        mockMvc.perform(patch("/api/v1/admin/reviews/{id}/status", reviewId)
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HIDDEN"))
                .andExpect(jsonPath("$.moderationReason").value("Inappropriate content"));
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/reviews/{id}/status as MANAGER returns 403 Forbidden (Owner only)")
    void testModerateReview_Manager_Returns403() throws Exception {
        UUID reviewId = UUID.randomUUID();
        ModerateReviewRequest request = new ModerateReviewRequest(ReviewStatus.HIDDEN, "Inappropriate content");

        UserPrincipal principal = new UserPrincipal(UUID.randomUUID(), "manager", "manager@test.com", Role.MANAGER);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        mockMvc.perform(patch("/api/v1/admin/reviews/{id}/status", reviewId)
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
