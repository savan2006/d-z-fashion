package com.dzfashion.commerce.controller;

import com.dzfashion.commerce.dto.response.AuditLogResponse;
import com.dzfashion.commerce.dto.response.PageResponse;
import com.dzfashion.commerce.enums.Role;
import com.dzfashion.commerce.security.JwtAuthenticationFilter;
import com.dzfashion.commerce.security.JwtTokenService;
import com.dzfashion.commerce.security.SecurityConfig;
import com.dzfashion.commerce.security.UserPrincipal;
import com.dzfashion.commerce.service.AuditService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminAuditLogController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AdminAuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditService auditService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @Test
    @DisplayName("Owner can access audit logs returning 200 OK")
    void testGetAuditLogs_Owner_Returns200() throws Exception {
        UUID ownerId = UUID.randomUUID();
        AuditLogResponse logResponse = new AuditLogResponse(
                UUID.randomUUID(), ownerId, "OWNER", "owner@dzfashion.com",
                "UPDATE_PRICE", "Product", "KURTI-01", "Updated price to 1499", "127.0.0.1", Instant.now()
        );

        when(auditService.getAuditLogs(any(), any(), any(), any()))
                .thenReturn(new PageResponse<>(List.of(logResponse), 0, 25, 1, 1, true, true));

        UserPrincipal principal = new UserPrincipal(ownerId, "owner", "owner@dzfashion.com", Role.OWNER);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].action").value("UPDATE_PRICE"));
    }

    @Test
    @DisplayName("Manager cannot access audit logs returning 403 Forbidden (Owner only)")
    void testGetAuditLogs_Manager_Returns403() throws Exception {
        UserPrincipal principal = new UserPrincipal(UUID.randomUUID(), "manager", "manager@dzfashion.com", Role.MANAGER);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .with(authentication(auth)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Customer cannot access audit logs returning 403 Forbidden")
    void testGetAuditLogs_Customer_Returns403() throws Exception {
        UserPrincipal principal = new UserPrincipal(UUID.randomUUID(), "customer", "customer@dzfashion.com", Role.CUSTOMER);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .with(authentication(auth)))
                .andExpect(status().isForbidden());
    }
}
