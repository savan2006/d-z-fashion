package com.dzfashion.fulfillment.controller;

import com.dzfashion.fulfillment.dto.request.CreateTransportAgencyRequest;
import com.dzfashion.fulfillment.dto.request.UpdateTransportAgencyRequest;
import com.dzfashion.fulfillment.dto.response.TransportAgencyResponse;
import com.dzfashion.fulfillment.enums.Role;
import com.dzfashion.fulfillment.security.JwtAuthenticationFilter;
import com.dzfashion.fulfillment.security.JwtTokenService;
import com.dzfashion.fulfillment.security.SecurityConfig;
import com.dzfashion.fulfillment.security.UserPrincipal;
import com.dzfashion.fulfillment.service.TransportAgencyService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransportAgencyController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class TransportAgencyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private TransportAgencyService transportAgencyService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    private UsernamePasswordAuthenticationToken createAuth(UUID id, String username, Role role) {
        UserPrincipal principal = new UserPrincipal(id, username, username + "@example.com", role);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    @Test
    @DisplayName("POST /api/v1/fulfillment/agencies allows MANAGER to create transport agency")
    void testCreateAgency_ManagerAllowed() throws Exception {
        CreateTransportAgencyRequest request = new CreateTransportAgencyRequest("Delhivery", "Lead", "9876543210");
        TransportAgencyResponse response = new TransportAgencyResponse(
                UUID.randomUUID(), "Delhivery", "Lead", "9876543210", true, Instant.now()
        );

        when(transportAgencyService.createAgency(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/fulfillment/agencies")
                        .with(authentication(createAuth(UUID.randomUUID(), "manager1", Role.MANAGER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Delhivery"));
    }

    @Test
    @DisplayName("PUT /api/v1/fulfillment/agencies/{id} allows MANAGER to update agency")
    void testUpdateAgency_ManagerAllowed() throws Exception {
        UUID agencyId = UUID.randomUUID();
        UpdateTransportAgencyRequest request = new UpdateTransportAgencyRequest("New Lead", "9876500000", false);
        TransportAgencyResponse response = new TransportAgencyResponse(
                agencyId, "Delhivery", "New Lead", "9876500000", false, Instant.now()
        );

        when(transportAgencyService.updateAgency(eq(agencyId), any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/fulfillment/agencies/" + agencyId)
                        .with(authentication(createAuth(UUID.randomUUID(), "manager1", Role.MANAGER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/fulfillment/agencies/active returns active agencies")
    void testGetActiveAgencies() throws Exception {
        TransportAgencyResponse response = new TransportAgencyResponse(
                UUID.randomUUID(), "Delhivery", "Lead", "9876543210", true, Instant.now()
        );
        when(transportAgencyService.getActiveAgencies()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/fulfillment/agencies/active")
                        .with(authentication(createAuth(UUID.randomUUID(), "manager1", Role.MANAGER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Delhivery"));
    }

    @Test
    @DisplayName("POST /api/v1/fulfillment/agencies denies CUSTOMER access (403)")
    void testCreateAgency_CustomerDenied() throws Exception {
        CreateTransportAgencyRequest request = new CreateTransportAgencyRequest("Delhivery", "Lead", "9876543210");

        mockMvc.perform(post("/api/v1/fulfillment/agencies")
                        .with(authentication(createAuth(UUID.randomUUID(), "customer1", Role.CUSTOMER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
