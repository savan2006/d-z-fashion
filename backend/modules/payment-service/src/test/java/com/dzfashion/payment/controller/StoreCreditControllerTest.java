package com.dzfashion.payment.controller;

import com.dzfashion.payment.dto.request.IssueStoreCreditRequest;
import com.dzfashion.payment.dto.request.RedeemStoreCreditRequest;
import com.dzfashion.payment.dto.response.StoreCreditBalanceResponse;
import com.dzfashion.payment.dto.response.StoreCreditEntryResponse;
import com.dzfashion.payment.enums.Role;
import com.dzfashion.payment.enums.StoreCreditEntryType;
import com.dzfashion.payment.enums.StoreCreditReferenceType;
import com.dzfashion.payment.security.JwtAuthenticationFilter;
import com.dzfashion.payment.security.JwtTokenService;
import com.dzfashion.payment.security.SecurityConfig;
import com.dzfashion.payment.security.UserPrincipal;
import com.dzfashion.payment.service.StoreCreditService;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StoreCreditController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class StoreCreditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private StoreCreditService storeCreditService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    private UsernamePasswordAuthenticationToken createAuth(UUID id, String username, Role role) {
        UserPrincipal principal = new UserPrincipal(id, username, username + "@example.com", role);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    @Test
    @DisplayName("GET /api/v1/store-credit/my-balance returns balance for customer")
    void getMyBalance_Customer_Success() throws Exception {
        UUID customerId = UUID.randomUUID();
        when(storeCreditService.getBalance(customerId))
                .thenReturn(new StoreCreditBalanceResponse(customerId, new BigDecimal("750.00")));

        mockMvc.perform(get("/api/v1/store-credit/my-balance")
                        .with(authentication(createAuth(customerId, "customer1", Role.CUSTOMER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.currentBalance").value(750.00));
    }

    @Test
    @DisplayName("POST /api/v1/store-credit/issue allows OWNER to issue store credit")
    void issueStoreCredit_Owner_Success() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        IssueStoreCreditRequest request = new IssueStoreCreditRequest(
                customerId,
                new BigDecimal("500.00"),
                StoreCreditReferenceType.MANUAL_ADJUSTMENT,
                "REF-001",
                "Loyalty bonus"
        );

        StoreCreditEntryResponse response = new StoreCreditEntryResponse(
                UUID.randomUUID(),
                customerId,
                new BigDecimal("500.00"),
                new BigDecimal("500.00"),
                StoreCreditEntryType.CREDIT_ISSUED,
                StoreCreditReferenceType.MANUAL_ADJUSTMENT,
                "REF-001",
                "Loyalty bonus",
                Instant.now()
        );

        when(storeCreditService.issueStoreCredit(any(IssueStoreCreditRequest.class), eq(ownerId)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/store-credit/issue")
                        .with(authentication(createAuth(ownerId, "owner1", Role.OWNER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(500.00))
                .andExpect(jsonPath("$.entryType").value("CREDIT_ISSUED"));
    }

    @Test
    @DisplayName("POST /api/v1/store-credit/issue forbidden for MANAGER")
    void issueStoreCredit_Manager_Forbidden() throws Exception {
        IssueStoreCreditRequest request = new IssueStoreCreditRequest(
                UUID.randomUUID(),
                new BigDecimal("500.00"),
                StoreCreditReferenceType.MANUAL_ADJUSTMENT,
                "REF-002",
                "Unauthorized attempt"
        );

        mockMvc.perform(post("/api/v1/store-credit/issue")
                        .with(authentication(createAuth(UUID.randomUUID(), "manager1", Role.MANAGER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/store-credit/redeem redeems store credit during checkout")
    void redeemStoreCredit_Success() throws Exception {
        UUID customerId = UUID.randomUUID();
        RedeemStoreCreditRequest request = new RedeemStoreCreditRequest(customerId, new BigDecimal("200.00"), "ORD-20261116-01");

        StoreCreditEntryResponse response = new StoreCreditEntryResponse(
                UUID.randomUUID(),
                customerId,
                new BigDecimal("200.00"),
                new BigDecimal("300.00"),
                StoreCreditEntryType.CREDIT_USED,
                StoreCreditReferenceType.ORDER,
                "ORD-20261116-01",
                "Redeemed for order",
                Instant.now()
        );

        when(storeCreditService.redeemStoreCredit(any(RedeemStoreCreditRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/store-credit/redeem")
                        .with(authentication(createAuth(customerId, "customer1", Role.CUSTOMER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entryType").value("CREDIT_USED"))
                .andExpect(jsonPath("$.balanceAfter").value(300.00));
    }
}
