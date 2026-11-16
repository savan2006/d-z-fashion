package com.dzfashion.commerce.controller;

import com.dzfashion.commerce.dto.response.ManagerOperationalReportResponse;
import com.dzfashion.commerce.dto.response.OwnerAnalyticsReportResponse;
import com.dzfashion.commerce.enums.Role;
import com.dzfashion.commerce.security.JwtAuthenticationFilter;
import com.dzfashion.commerce.security.JwtTokenService;
import com.dzfashion.commerce.security.SecurityConfig;
import com.dzfashion.commerce.security.UserPrincipal;
import com.dzfashion.commerce.service.ReportingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportingController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class ReportingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportingService reportingService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @Test
    @DisplayName("Manager can access operational reports without revenue figures")
    void testOperationalReport_Manager_Returns200() throws Exception {
        ManagerOperationalReportResponse response = new ManagerOperationalReportResponse(
                50, 10, 15, 8, 12, 5, 4, 1, 2
        );
        when(reportingService.getManagerOperationalReport()).thenReturn(response);

        UserPrincipal principal = new UserPrincipal(UUID.randomUUID(), "manager", "manager@dzfashion.com", Role.MANAGER);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        mockMvc.perform(get("/api/v1/admin/reports/operational")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders").value(50))
                .andExpect(jsonPath("$.pendingClaimOrders").value(10))
                .andExpect(jsonPath("$.openSupportTickets").value(4));
    }

    @Test
    @DisplayName("Manager cannot access financial analytics report returning 403 Forbidden")
    void testAnalyticsReport_Manager_Returns403() throws Exception {
        UserPrincipal principal = new UserPrincipal(UUID.randomUUID(), "manager", "manager@dzfashion.com", Role.MANAGER);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        mockMvc.perform(get("/api/v1/admin/reports/analytics")
                        .with(authentication(auth)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Owner can access complete financial analytics report returning 200 OK")
    void testAnalyticsReport_Owner_Returns200() throws Exception {
        OwnerAnalyticsReportResponse response = new OwnerAnalyticsReportResponse(
                100, new BigDecimal("250000.00"), new BigDecimal("15000.00"), new BigDecimal("235000.00"),
                60, new BigDecimal("150000.00"), 35, new BigDecimal("85000.00"),
                5, 3, 0, 1
        );
        when(reportingService.getOwnerAnalyticsReport()).thenReturn(response);

        UserPrincipal principal = new UserPrincipal(UUID.randomUUID(), "owner", "owner@dzfashion.com", Role.OWNER);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        mockMvc.perform(get("/api/v1/admin/reports/analytics")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders").value(100))
                .andExpect(jsonPath("$.grossMerchandiseValue").value(250000.00))
                .andExpect(jsonPath("$.netSalesRevenue").value(235000.00));
    }

    @Test
    @DisplayName("Customer cannot access operational or analytics reports returning 403 Forbidden")
    void testReports_Customer_Returns403() throws Exception {
        UserPrincipal principal = new UserPrincipal(UUID.randomUUID(), "customer", "customer@dzfashion.com", Role.CUSTOMER);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        mockMvc.perform(get("/api/v1/admin/reports/operational")
                        .with(authentication(auth)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/reports/analytics")
                        .with(authentication(auth)))
                .andExpect(status().isForbidden());
    }
}
