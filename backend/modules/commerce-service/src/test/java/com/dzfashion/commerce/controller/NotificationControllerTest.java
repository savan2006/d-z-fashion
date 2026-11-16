package com.dzfashion.commerce.controller;

import com.dzfashion.commerce.dto.response.NotificationResponse;
import com.dzfashion.commerce.dto.response.PageResponse;
import com.dzfashion.commerce.dto.response.UnreadNotificationCountResponse;
import com.dzfashion.commerce.enums.NotificationType;
import com.dzfashion.commerce.enums.Role;
import com.dzfashion.commerce.security.JwtAuthenticationFilter;
import com.dzfashion.commerce.security.JwtTokenService;
import com.dzfashion.commerce.security.SecurityConfig;
import com.dzfashion.commerce.security.UserPrincipal;
import com.dzfashion.commerce.service.NotificationService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @Test
    @DisplayName("Authenticated user gets notifications list")
    void testGetNotifications_Authenticated_Returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        when(notificationService.getNotificationsForRecipient(eq(userId), any()))
                .thenReturn(new PageResponse<>(List.of(), 0, 15, 0, 0, true, true));

        UserPrincipal principal = new UserPrincipal(userId, "customer", "customer@dzfashion.com", Role.CUSTOMER);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        mockMvc.perform(get("/api/v1/notifications")
                        .with(authentication(auth)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("User gets unread notifications count")
    void testGetUnreadCount_Returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        when(notificationService.getUnreadCount(userId)).thenReturn(new UnreadNotificationCountResponse(4));

        UserPrincipal principal = new UserPrincipal(userId, "customer", "customer@dzfashion.com", Role.CUSTOMER);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(4));
    }

    @Test
    @DisplayName("User marks single notification read")
    void testMarkAsRead_Returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID notifId = UUID.randomUUID();

        NotificationResponse response = new NotificationResponse(
                notifId, userId, null, NotificationType.ORDER_CONFIRMED,
                "Order Placed", "ORD-001 placed", "ORD-001", "Order",
                true, Instant.now(), Instant.now()
        );

        when(notificationService.markAsRead(notifId, userId)).thenReturn(response);

        UserPrincipal principal = new UserPrincipal(userId, "customer", "customer@dzfashion.com", Role.CUSTOMER);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        mockMvc.perform(patch("/api/v1/notifications/{id}/read", notifId)
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));
    }
}
