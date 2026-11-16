package com.dzfashion.commerce.controller;

import com.dzfashion.commerce.dto.request.AddTicketMessageRequest;
import com.dzfashion.commerce.dto.request.CreateSupportTicketRequest;
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

@WebMvcTest(CustomerSupportController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class CustomerSupportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private SupportTicketService supportTicketService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @Test
    @DisplayName("Unauthenticated request to support tickets returns 401")
    void testSupport_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/support/tickets"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Customer creates support ticket returning 201 Created")
    void testCreateTicket_Customer_Returns201() throws Exception {
        UUID customerId = UUID.randomUUID();
        CreateSupportTicketRequest request = new CreateSupportTicketRequest(
                UUID.randomUUID(), "Fabric inquiry", "Is this pure mulberry silk?", SupportTicketPriority.MEDIUM
        );

        SupportTicketResponse response = new SupportTicketResponse(
                UUID.randomUUID(), "TCK-20261116-A1B2C3", customerId, request.orderId(), request.subject(),
                SupportTicketChannel.WEB, SupportTicketPriority.MEDIUM, SupportTicketStatus.OPEN,
                null, false, null, null, null, Instant.now(), Instant.now(), List.of()
        );

        when(supportTicketService.createTicket(eq(customerId), any())).thenReturn(response);

        UserPrincipal principal = new UserPrincipal(customerId, "customer", "customer@dzfashion.com", Role.CUSTOMER);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        mockMvc.perform(post("/api/v1/support/tickets")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketNumber").value("TCK-20261116-A1B2C3"))
                .andExpect(jsonPath("$.subject").value("Fabric inquiry"));
    }

    @Test
    @DisplayName("Customer lists own tickets returning 200 OK")
    void testGetMyTickets_Customer_Returns200() throws Exception {
        UUID customerId = UUID.randomUUID();
        when(supportTicketService.getTicketsForCustomer(eq(customerId), any()))
                .thenReturn(new PageResponse<>(List.of(), 0, 10, 0, 0, true, true));

        UserPrincipal principal = new UserPrincipal(customerId, "customer", "customer@dzfashion.com", Role.CUSTOMER);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        mockMvc.perform(get("/api/v1/support/tickets")
                        .with(authentication(auth)))
                .andExpect(status().isOk());
    }
}
