package com.dzfashion.catalog.controller;

import com.dzfashion.catalog.dto.request.ReviewRevisionRequest;
import com.dzfashion.catalog.dto.request.UpdateProductPriceRequest;
import com.dzfashion.catalog.dto.response.ProductResponse;
import com.dzfashion.catalog.dto.response.ProductRevisionResponse;
import com.dzfashion.catalog.enums.ProductStatus;
import com.dzfashion.catalog.enums.RevisionStatus;
import com.dzfashion.catalog.enums.Role;
import com.dzfashion.catalog.security.JwtAuthenticationFilter;
import com.dzfashion.catalog.security.JwtTokenService;
import com.dzfashion.catalog.security.SecurityConfig;
import com.dzfashion.catalog.security.UserPrincipal;
import com.dzfashion.catalog.service.ProductRevisionService;
import com.dzfashion.catalog.service.ProductService;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductAdminController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class ProductAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private ProductRevisionService revisionService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @Test
    @DisplayName("Unauthenticated request to update price returns 401 Unauthorized")
    void updatePriceUnauthenticated() throws Exception {
        UUID id = UUID.randomUUID();
        UpdateProductPriceRequest request = new UpdateProductPriceRequest(new BigDecimal("999.00"));

        mockMvc.perform(put("/api/v1/admin/products/" + id + "/price")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("MANAGER role is forbidden from updating product price (403 Forbidden)")
    void managerDeniedFromPriceUpdate() throws Exception {
        UUID id = UUID.randomUUID();
        UpdateProductPriceRequest request = new UpdateProductPriceRequest(new BigDecimal("999.00"));

        mockMvc.perform(put("/api/v1/admin/products/" + id + "/price")
                        .with(user("manager1").roles("MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("OWNER role can update product price (200 OK)")
    void ownerCanUpdatePrice() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(ownerId, "owner1", "owner@dzfashion.com", Role.OWNER);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        UUID productId = UUID.randomUUID();
        UpdateProductPriceRequest request = new UpdateProductPriceRequest(new BigDecimal("1499.00"));

        when(productService.updateProductPrice(eq(productId), any(), eq(ownerId))).thenReturn(
                new ProductResponse(productId, UUID.randomUUID(), "Kurtas", "kurtas", "Festive Kurta", "festive-kurta",
                        null, "Silk", "Party", ProductStatus.PUBLISHED, new BigDecimal("1499.00"), List.of(), List.of(),
                        Instant.now(), Instant.now())
        );

        mockMvc.perform(put("/api/v1/admin/products/" + productId + "/price")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultPrice").value(1499.00));
    }

    @Test
    @DisplayName("OWNER role can review and approve a product revision (200 OK)")
    void ownerCanApproveRevision() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(ownerId, "owner1", "owner@dzfashion.com", Role.OWNER);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        UUID revisionId = UUID.randomUUID();
        ReviewRevisionRequest request = new ReviewRevisionRequest(true, null);

        when(revisionService.reviewRevision(eq(revisionId), eq(ownerId), any())).thenReturn(
                new ProductRevisionResponse(revisionId, UUID.randomUUID(), "Kurta", UUID.randomUUID(), ownerId,
                        "{}", RevisionStatus.APPROVED, null, Instant.now(), Instant.now())
        );

        mockMvc.perform(post("/api/v1/admin/products/revisions/" + revisionId + "/review")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }
}
