package com.dzfashion.payment.controller;

import com.dzfashion.payment.dto.request.ApplyPromotionRequest;
import com.dzfashion.payment.dto.request.CreatePromotionRequest;
import com.dzfashion.payment.dto.response.PromotionEvaluationResponse;
import com.dzfashion.payment.dto.response.PromotionResponse;
import com.dzfashion.payment.enums.PromotionType;
import com.dzfashion.payment.enums.Role;
import com.dzfashion.payment.security.JwtAuthenticationFilter;
import com.dzfashion.payment.security.JwtTokenService;
import com.dzfashion.payment.security.SecurityConfig;
import com.dzfashion.payment.security.UserPrincipal;
import com.dzfashion.payment.service.PromotionService;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PromotionController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class PromotionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @MockitoBean
    private PromotionService promotionService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    private UsernamePasswordAuthenticationToken createAuth(UUID id, String username, Role role) {
        UserPrincipal principal = new UserPrincipal(id, username, username + "@example.com", role);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    @Test
    @DisplayName("POST /api/v1/promotions/evaluate evaluates promotion code")
    void evaluatePromotion_Success() throws Exception {
        ApplyPromotionRequest request = new ApplyPromotionRequest("SUMMER10", new BigDecimal("3000.00"));
        PromotionEvaluationResponse response = PromotionEvaluationResponse.applied(
                UUID.randomUUID(), "SUMMER10", new BigDecimal("300.00"));

        when(promotionService.evaluatePromotion("SUMMER10", new BigDecimal("3000.00"))).thenReturn(response);

        mockMvc.perform(post("/api/v1/promotions/evaluate")
                        .with(authentication(createAuth(UUID.randomUUID(), "customer1", Role.CUSTOMER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.code").value("SUMMER10"))
                .andExpect(jsonPath("$.discountAmount").value(300.00));
    }

    @Test
    @DisplayName("POST /api/v1/admin/promotions creates promotion for OWNER")
    void createPromotion_Owner_Success() throws Exception {
        UUID ownerId = UUID.randomUUID();
        CreatePromotionRequest request = new CreatePromotionRequest(
                "DIWALI500",
                "Diwali ₹500 Off",
                PromotionType.FIXED_AMOUNT,
                new BigDecimal("500.00"),
                new BigDecimal("2500.00"),
                null,
                Instant.now(),
                Instant.now().plus(30, ChronoUnit.DAYS),
                200
        );

        PromotionResponse response = new PromotionResponse(
                UUID.randomUUID(),
                "DIWALI500",
                "Diwali ₹500 Off",
                PromotionType.FIXED_AMOUNT,
                new BigDecimal("500.00"),
                new BigDecimal("2500.00"),
                null,
                Instant.now(),
                Instant.now().plus(30, ChronoUnit.DAYS),
                200,
                0,
                true,
                Instant.now()
        );

        when(promotionService.createPromotion(any(CreatePromotionRequest.class), eq(ownerId)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/promotions")
                        .with(authentication(createAuth(ownerId, "owner1", Role.OWNER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("DIWALI500"))
                .andExpect(jsonPath("$.discountValue").value(500.00));
    }

    @Test
    @DisplayName("POST /api/v1/admin/promotions is forbidden for MANAGER")
    void createPromotion_Manager_Forbidden() throws Exception {
        CreatePromotionRequest request = new CreatePromotionRequest(
                "UNAUTHORIZED",
                "Manager attempt",
                PromotionType.FIXED_AMOUNT,
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                null,
                Instant.now(),
                Instant.now().plus(10, ChronoUnit.DAYS),
                null
        );

        mockMvc.perform(post("/api/v1/admin/promotions")
                        .with(authentication(createAuth(UUID.randomUUID(), "manager1", Role.MANAGER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/v1/admin/promotions/{id} allows OWNER to deactivate promotion")
    void deactivatePromotion_Owner_Success() throws Exception {
        UUID promoId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/admin/promotions/" + promoId)
                        .with(authentication(createAuth(UUID.randomUUID(), "owner1", Role.OWNER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
