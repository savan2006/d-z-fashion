package com.dzfashion.identity.controller;

import com.dzfashion.identity.dto.request.CreateManagerRequest;
import com.dzfashion.identity.dto.response.ActionResponse;
import com.dzfashion.identity.dto.response.UserSummaryResponse;
import com.dzfashion.identity.enums.Role;
import com.dzfashion.identity.enums.UserStatus;
import com.dzfashion.identity.security.JwtAuthenticationFilter;
import com.dzfashion.identity.security.JwtTokenService;
import com.dzfashion.identity.security.SecurityConfig;
import com.dzfashion.identity.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ManagerAdminController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class ManagerAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @Test
    @DisplayName("Unauthenticated request to list managers returns 401 Unauthorized")
    void testListManagersUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/admin/managers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CUSTOMER role is denied from Owner manager administration (403 Forbidden)")
    void testCustomerDeniedFromManagerAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/managers").with(user("customer1").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("MANAGER role is denied from Owner manager administration (403 Forbidden)")
    void testManagerDeniedFromManagerAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/managers").with(user("manager1").roles("MANAGER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("OWNER role is permitted to list managers (200 OK)")
    void testOwnerCanListManagers() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.listManagers()).thenReturn(List.of(
                new UserSummaryResponse(id, "mgr1", "mgr1@dzfashion.com", Role.MANAGER, UserStatus.ACTIVE)
        ));

        mockMvc.perform(get("/api/v1/admin/managers").with(user("owner1").roles("OWNER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("mgr1"))
                .andExpect(jsonPath("$[0].role").value("MANAGER"));
    }

    @Test
    @DisplayName("OWNER role is permitted to create a new manager (201 Created)")
    void testOwnerCanCreateManager() throws Exception {
        CreateManagerRequest request = new CreateManagerRequest("mgr_new", "mgr_new@dzfashion.com", "Password123", "New Manager");
        UUID id = UUID.randomUUID();

        when(userService.createManager(any(CreateManagerRequest.class))).thenReturn(
                new UserSummaryResponse(id, "mgr_new", "mgr_new@dzfashion.com", Role.MANAGER, UserStatus.ACTIVE)
        );

        mockMvc.perform(post("/api/v1/admin/managers")
                        .with(user("owner1").roles("OWNER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("mgr_new"))
                .andExpect(jsonPath("$.role").value("MANAGER"));
    }

    @Test
    @DisplayName("OWNER role can reset manager password")
    void testOwnerCanResetManagerPassword() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.resetManagerPassword(id, "NewSecurePassword123"))
                .thenReturn(new ActionResponse("Manager password has been reset successfully"));

        mockMvc.perform(post("/api/v1/admin/managers/" + id + "/reset-password")
                        .with(user("owner1").roles("OWNER"))
                        .param("newPassword", "NewSecurePassword123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Manager password has been reset successfully"));
    }
}
