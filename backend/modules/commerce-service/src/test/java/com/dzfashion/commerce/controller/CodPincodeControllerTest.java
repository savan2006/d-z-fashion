package com.dzfashion.commerce.controller;

import com.dzfashion.commerce.dto.request.AddCodPincodeRequest;
import com.dzfashion.commerce.dto.response.CodPincodeResponse;
import com.dzfashion.commerce.enums.Role;
import com.dzfashion.commerce.security.JwtAuthenticationFilter;
import com.dzfashion.commerce.security.JwtTokenService;
import com.dzfashion.commerce.security.SecurityConfig;
import com.dzfashion.commerce.security.UserPrincipal;
import com.dzfashion.commerce.service.CodPincodeService;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CodPincodeController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class CodPincodeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private CodPincodeService codPincodeService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    private UsernamePasswordAuthenticationToken createAuth(UUID id, String username, Role role) {
        UserPrincipal principal = new UserPrincipal(id, username, username + "@example.com", role);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    @Test
    @DisplayName("Public can check COD pincode serviceability without authentication")
    void checkPincodePublicAccess() throws Exception {
        when(codPincodeService.isPincodeServiceable("395001")).thenReturn(true);

        mockMvc.perform(get("/api/v1/cod-pincodes/check/395001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pincode").value("395001"))
                .andExpect(jsonPath("$.serviceable").value(true));
    }

    @Test
    @DisplayName("Owner can add new COD pincode")
    void ownerCanAddPincode() throws Exception {
        UUID ownerId = UUID.randomUUID();
        AddCodPincodeRequest request = new AddCodPincodeRequest("395007");

        CodPincodeResponse response = new CodPincodeResponse(
                UUID.randomUUID(), "395007", true, ownerId, Instant.now()
        );
        when(codPincodeService.addPincode(eq(request), eq(ownerId))).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/cod-pincodes")
                        .with(authentication(createAuth(ownerId, "owner1", Role.OWNER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pincode").value("395007"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @DisplayName("Customer cannot add COD pincode (403 Forbidden)")
    void customerCannotAddPincode() throws Exception {
        UUID customerId = UUID.randomUUID();
        AddCodPincodeRequest request = new AddCodPincodeRequest("395007");

        mockMvc.perform(post("/api/v1/admin/cod-pincodes")
                        .with(authentication(createAuth(customerId, "cust1", Role.CUSTOMER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
