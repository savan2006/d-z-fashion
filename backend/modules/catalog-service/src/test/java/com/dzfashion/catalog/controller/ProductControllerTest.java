package com.dzfashion.catalog.controller;

import com.dzfashion.catalog.dto.request.CreateProductDraftRequest;
import com.dzfashion.catalog.dto.request.CreateVariantDraftRequest;
import com.dzfashion.catalog.dto.request.SubmitRevisionRequest;
import com.dzfashion.catalog.dto.response.PageResponse;
import com.dzfashion.catalog.dto.response.ProductResponse;
import com.dzfashion.catalog.dto.response.ProductRevisionResponse;
import com.dzfashion.catalog.dto.response.ProductSummaryResponse;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class ProductControllerTest {

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
    @DisplayName("Public GET /api/v1/products returns paginated products without authentication")
    void listProductsPublic() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.searchAndFilterProducts(any(), any())).thenReturn(
                new PageResponse<>(List.of(
                        new ProductSummaryResponse(id, "Kurtas", "Anarkali Kurta", "anarkali-kurta", "Cotton", "Festive", new BigDecimal("1299.00"), "https://img.url/1.jpg", ProductStatus.PUBLISHED)
                ), 0, 20, 1, 1, true)
        );

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Anarkali Kurta"))
                .andExpect(jsonPath("$.content[0].defaultPrice").value(1299.00));
    }

    @Test
    @DisplayName("CUSTOMER role is forbidden from creating product draft (403 Forbidden)")
    void customerDeniedFromCreatingDraft() throws Exception {
        CreateProductDraftRequest request = new CreateProductDraftRequest(
                "Draft Kurta", "draft-kurta", UUID.randomUUID(), null, null, null, List.of(), List.of()
        );

        mockMvc.perform(post("/api/v1/products/draft")
                        .with(user("customer1").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("MANAGER role can create product draft (201 Created)")
    void managerCanCreateDraft() throws Exception {
        UUID managerId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(managerId, "mgr1", "mgr1@dzfashion.com", Role.MANAGER);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        UUID categoryId = UUID.randomUUID();
        CreateProductDraftRequest request = new CreateProductDraftRequest(
                "Draft Kurta", "draft-kurta", categoryId, "Desc", "Cotton", "Daily",
                List.of(new CreateVariantDraftRequest("M", "Red")), List.of()
        );

        UUID productId = UUID.randomUUID();
        when(productService.createProductDraft(any(), eq(managerId))).thenReturn(
                new ProductResponse(productId, categoryId, "Kurtas", "kurtas", "Draft Kurta", "draft-kurta",
                        "Desc", "Cotton", "Daily", ProductStatus.DRAFT, BigDecimal.ZERO, List.of(), List.of(),
                        Instant.now(), Instant.now())
        );

        mockMvc.perform(post("/api/v1/products/draft")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Draft Kurta"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    @DisplayName("MANAGER role can submit revision for an existing product (201 Created)")
    void managerCanSubmitRevision() throws Exception {
        UUID managerId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(managerId, "mgr1", "mgr1@dzfashion.com", Role.MANAGER);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        UUID productId = UUID.randomUUID();
        SubmitRevisionRequest request = new SubmitRevisionRequest(
                "Updated Name", "Updated desc", "Silk", "Party", null, List.of(), List.of()
        );

        UUID revisionId = UUID.randomUUID();
        when(revisionService.submitRevision(eq(productId), eq(managerId), any())).thenReturn(
                new ProductRevisionResponse(revisionId, productId, "Draft Kurta", managerId, null,
                        "{}", RevisionStatus.PENDING, null, Instant.now(), null)
        );

        mockMvc.perform(post("/api/v1/products/" + productId + "/revisions")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }
}
