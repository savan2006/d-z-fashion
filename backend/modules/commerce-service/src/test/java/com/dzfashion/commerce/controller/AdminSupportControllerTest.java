package com.dzfashion.commerce.controller;

import com.dzfashion.commerce.dto.request.EscalateTicketRequest;
import com.dzfashion.commerce.dto.request.ManualSupportIntakeRequest;
import com.dzfashion.commerce.dto.response.PageResponse;
import com.dzfashion.commerce.dto.response.SupportTicketResponse;
import com.dzfashion.commerce.enums.Role;
import com.dzfashion.commerce.enums.SupportTicketChannel;
import com.dzfashion.commerce.enums.SupportTicketPriority;
import com.dzfashion.commerce.enums.SupportTicketStatus;
import com.dzfashion.commerce.security.JwtAuthenticationFilter;
import com.dzfashion.commerce.security.JwtTokenService;
import com.dzfashion.commerce.security.SecurityConfig;
import com.dzfashion.commerce.security.UserPrincipal;
import com.dzfashion.commerce.service.SupportTicketService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminSupportController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AdminSupportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private SupportTicketService supportTicketService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @Test
    @DisplayName("Manager can view ticket queue returning 200 OK")
    void testGetTickets_Manager_Returns200() throws Exception {
        when(supportTicketService.getAllTicketsForStaff(any(), any(), any(), any()))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true, true));

        UserPrincipal principal = new UserPrincipal(UUID.randomUUID(), "manager", "manager@dzfashion.com", Role.MANAGER);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        mockMvc.perform(get("/api/v1/admin/support/tickets")
                        .with(authentication(auth)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Customer cannot access admin support tickets endpoint returning 403 Forbidden")
    void testGetTickets_Customer_Returns403() throws Exception {
        UserPrincipal principal = new UserPrincipal(UUID.randomUUID(), "customer", "customer@dzfashion.com", Role.CUSTOMER);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        mockMvc.perform(get("/api/v1/admin/support/tickets")
                        .with(authentication(auth)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Manager records manual inquiry intake returning 201 Created")
    void testManualIntake_Manager_Returns201() throws Exception {
        UUID staffId = UUID.randomUUID();
        ManualSupportIntakeRequest request = new ManualSupportIntakeRequest(
                UUID.randomUUID(), null, SupportTicketChannel.PHONE, "Phone order status check",
                "Customer called asking about shipment dispatch", SupportTicketPriority.MEDIUM
        );

        SupportTicketResponse response = new SupportTicketResponse(
                UUID.randomUUID(), "TCK-20261116-PH001", request.customerId(), null, request.subject(),
                SupportTicketChannel.PHONE, SupportTicketPriority.MEDIUM, SupportTicketStatus.IN_PROGRESS,
                staffId, false, null, null, null, Instant.now(), Instant.now(), List.of()
        );

        when(supportTicketService.recordManualIntake(any(), eq(staffId), eq("MANAGER"))).thenReturn(response);

        UserPrincipal principal = new UserPrincipal(staffId, "manager", "manager@dzfashion.com", Role.MANAGER);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        mockMvc.perform(post("/api/v1/admin/support/manual-intake")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.channel").value("PHONE"));
    }

    @Test
    @DisplayName("Owner can escalate a ticket returning 200 OK")
    void testEscalateTicket_Owner_Returns200() throws Exception {
        UUID ticketId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        EscalateTicketRequest request = new EscalateTicketRequest("Severe transit delay");

        SupportTicketResponse response = new SupportTicketResponse(
                ticketId, "TCK-20261116-ESC01", UUID.randomUUID(), null, "Delayed delivery",
                SupportTicketChannel.WEB, SupportTicketPriority.URGENT, SupportTicketStatus.ESCALATED,
                null, true, "Severe transit delay", Instant.now(), null, Instant.now(), Instant.now(), List.of()
        );

        when(supportTicketService.escalateTicket(eq(ticketId), any(), eq(ownerId))).thenReturn(response);

        UserPrincipal principal = new UserPrincipal(ownerId, "owner", "owner@dzfashion.com", Role.OWNER);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        mockMvc.perform(post("/api/v1/admin/support/tickets/{id}/escalate", ticketId)
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.escalated").value(true));
    }

    @Test
    @DisplayName("Manager cannot escalate a ticket returning 403 Forbidden (Owner only)")
    void testEscalateTicket_Manager_Returns403() throws Exception {
        UUID ticketId = UUID.randomUUID();
        EscalateTicketRequest request = new EscalateTicketRequest("Severe transit delay");

        UserPrincipal principal = new UserPrincipal(UUID.randomUUID(), "manager", "manager@dzfashion.com", Role.MANAGER);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        mockMvc.perform(post("/api/v1/admin/support/tickets/{id}/escalate", ticketId)
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
