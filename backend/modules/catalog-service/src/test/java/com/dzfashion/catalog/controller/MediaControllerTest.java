package com.dzfashion.catalog.controller;

import com.dzfashion.catalog.dto.response.MediaAssetResponse;
import com.dzfashion.catalog.dto.response.MediaSignatureResponse;
import com.dzfashion.catalog.security.JwtAuthenticationFilter;
import com.dzfashion.catalog.security.JwtTokenService;
import com.dzfashion.catalog.security.SecurityConfig;
import com.dzfashion.catalog.service.CloudinaryMediaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MediaController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class MediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CloudinaryMediaService mediaService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @Test
    @DisplayName("Unauthenticated request to media signature returns 401 Unauthorized")
    void getSignatureUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/media/signature"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("MANAGER role can obtain Cloudinary upload signature (200 OK)")
    void managerCanGetUploadSignature() throws Exception {
        when(mediaService.generateUploadSignature()).thenReturn(
                new MediaSignatureResponse(1700000000L, "mock_signature", "mock-key", "dzfashion", "dzfashion/catalog")
        );

        mockMvc.perform(get("/api/v1/catalog/media/signature").with(user("manager1").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cloudName").value("dzfashion"))
                .andExpect(jsonPath("$.apiKey").value("mock-key"))
                .andExpect(jsonPath("$.signature").value("mock_signature"));
    }

    @Test
    @DisplayName("Public can retrieve media gallery for a product (200 OK)")
    void getProductMediaPublic() throws Exception {
        UUID productId = UUID.randomUUID();
        when(mediaService.getProductMedia(productId)).thenReturn(List.of(
                new MediaAssetResponse(UUID.randomUUID(), null, "dzfashion/catalog/1",
                        "https://res.cloudinary.com/dzfashion/image/upload/1.jpg", "jpg", 1000, 1000, "Alt", 0, true)
        ));

        mockMvc.perform(get("/api/v1/catalog/media/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cloudinaryPublicId").value("dzfashion/catalog/1"));
    }
}
