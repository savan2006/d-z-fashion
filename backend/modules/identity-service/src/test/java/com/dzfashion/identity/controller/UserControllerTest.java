package com.dzfashion.identity.controller;

import com.dzfashion.identity.dto.response.UserProfileResponse;
import com.dzfashion.identity.enums.Role;
import com.dzfashion.identity.security.JwtAuthenticationFilter;
import com.dzfashion.identity.security.JwtTokenService;
import com.dzfashion.identity.security.SecurityConfig;
import com.dzfashion.identity.security.UserPrincipal;
import com.dzfashion.identity.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @Test
    @DisplayName("Unauthenticated request to /me returns 401 Unauthorized")
    void getMeWithoutAuthReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Authenticated request to /me returns current user profile")
    void getMeWithAuthReturnsProfile() throws Exception {
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(
                userId,
                "customer",
                "customer@dzfashion.com",
                "password123",
                Role.CUSTOMER,
                com.dzfashion.identity.enums.UserStatus.ACTIVE
        );
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        UserProfileResponse profileResponse = new UserProfileResponse(
                userId,
                "customer",
                "customer@dzfashion.com",
                Role.CUSTOMER,
                "Customer User",
                "9876543210",
                "123 Ring Road",
                "Near Textile Market",
                "Surat",
                "Gujarat",
                "395002",
                "India"
        );

        when(userService.getProfile(userId)).thenReturn(profileResponse);

        mockMvc.perform(get("/api/v1/users/me").with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("customer@dzfashion.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }
}
