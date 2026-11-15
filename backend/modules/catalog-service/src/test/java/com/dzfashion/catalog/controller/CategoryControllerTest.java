package com.dzfashion.catalog.controller;

import com.dzfashion.catalog.dto.request.CreateCategoryRequest;
import com.dzfashion.catalog.dto.response.ActionResponse;
import com.dzfashion.catalog.dto.response.CategoryResponse;
import com.dzfashion.catalog.security.JwtAuthenticationFilter;
import com.dzfashion.catalog.security.JwtTokenService;
import com.dzfashion.catalog.security.SecurityConfig;
import com.dzfashion.catalog.service.CategoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @Test
    @DisplayName("Public GET /api/v1/categories returns category tree without authentication")
    void getCategoriesPublic() throws Exception {
        UUID id = UUID.randomUUID();
        when(categoryService.getCategoryTree(false)).thenReturn(List.of(
                new CategoryResponse(id, null, "Kurtas", "kurtas", null, true, 1, List.of())
        ));

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Kurtas"))
                .andExpect(jsonPath("$[0].slug").value("kurtas"));
    }

    @Test
    @DisplayName("Unauthenticated POST /api/v1/categories returns 401 Unauthorized")
    void createCategoryUnauthenticated() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest("Kurtas", "kurtas", null, null, 1);

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CUSTOMER or MANAGER role is forbidden from creating category (403 Forbidden)")
    void createCategoryManagerForbidden() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest("Kurtas", "kurtas", null, null, 1);

        mockMvc.perform(post("/api/v1/categories")
                        .with(user("manager1").roles("MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("OWNER role can create category (201 Created)")
    void createCategoryOwnerSuccess() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest("Sarees", "sarees", "Silk Sarees", null, 1);
        UUID id = UUID.randomUUID();

        when(categoryService.createCategory(any(CreateCategoryRequest.class))).thenReturn(
                new CategoryResponse(id, null, "Sarees", "sarees", "Silk Sarees", true, 1, List.of())
        );

        mockMvc.perform(post("/api/v1/categories")
                        .with(user("owner1").roles("OWNER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Sarees"))
                .andExpect(jsonPath("$.slug").value("sarees"));
    }

    @Test
    @DisplayName("OWNER role can deactivate category (200 OK)")
    void deactivateCategoryOwnerSuccess() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/categories/" + id)
                        .with(user("owner1").roles("OWNER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Category and its children have been deactivated successfully"));
    }
}
