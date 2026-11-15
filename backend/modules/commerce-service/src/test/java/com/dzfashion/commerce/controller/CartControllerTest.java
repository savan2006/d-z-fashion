package com.dzfashion.commerce.controller;

import com.dzfashion.commerce.dto.request.AddCartItemRequest;
import com.dzfashion.commerce.dto.request.UpdateCartItemRequest;
import com.dzfashion.commerce.dto.response.CartItemResponse;
import com.dzfashion.commerce.dto.response.CartResponse;
import com.dzfashion.commerce.enums.Role;
import com.dzfashion.commerce.security.JwtAuthenticationFilter;
import com.dzfashion.commerce.security.JwtTokenService;
import com.dzfashion.commerce.security.SecurityConfig;
import com.dzfashion.commerce.security.UserPrincipal;
import com.dzfashion.commerce.service.CartService;
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
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    private UsernamePasswordAuthenticationToken createAuth(UUID id, String username, Role role) {
        UserPrincipal principal = new UserPrincipal(id, username, username + "@example.com", role);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    @Test
    @DisplayName("Unauthenticated request to get cart returns 401 Unauthorized")
    void getCartUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/cart"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Manager attempting to access customer cart returns 403 Forbidden")
    void managerCannotAccessCart() throws Exception {
        UUID managerId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/cart")
                        .with(authentication(createAuth(managerId, "manager1", Role.MANAGER))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Customer can get their cart")
    void customerGetCartSuccess() throws Exception {
        UUID customerId = UUID.randomUUID();
        CartResponse cart = new CartResponse(UUID.randomUUID(), customerId, Collections.emptyList(), 0, BigDecimal.ZERO, false);
        when(cartService.getCustomerCart(customerId)).thenReturn(cart);

        mockMvc.perform(get("/api/v1/cart")
                        .with(authentication(createAuth(customerId, "customer1", Role.CUSTOMER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    @DisplayName("Customer can add item to cart")
    void customerAddItemSuccess() throws Exception {
        UUID customerId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        AddCartItemRequest request = new AddCartItemRequest(variantId, 2);

        CartItemResponse item = new CartItemResponse(
                UUID.randomUUID(), UUID.randomUUID(), variantId, "Chanderi Suit", "DZF-SUIT-01",
                "M", "Teal", 2, new BigDecimal("1800.00"), new BigDecimal("1800.00"), false, true, new BigDecimal("3600.00")
        );
        CartResponse cart = new CartResponse(UUID.randomUUID(), customerId, List.of(item), 2, new BigDecimal("3600.00"), false);
        when(cartService.addItemToCart(eq(customerId), any())).thenReturn(cart);

        mockMvc.perform(post("/api/v1/cart/items")
                        .with(authentication(createAuth(customerId, "customer1", Role.CUSTOMER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].productName").value("Chanderi Suit"))
                .andExpect(jsonPath("$.totalItems").value(2));
    }

    @Test
    @DisplayName("Customer can update item quantity")
    void customerUpdateQuantitySuccess() throws Exception {
        UUID customerId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UpdateCartItemRequest request = new UpdateCartItemRequest(3);

        CartResponse cart = new CartResponse(UUID.randomUUID(), customerId, Collections.emptyList(), 3, new BigDecimal("5400.00"), false);
        when(cartService.updateItemQuantity(customerId, itemId, 3)).thenReturn(cart);

        mockMvc.perform(put("/api/v1/cart/items/" + itemId)
                        .with(authentication(createAuth(customerId, "customer1", Role.CUSTOMER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Customer can clear cart")
    void customerClearCartSuccess() throws Exception {
        UUID customerId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/cart")
                        .with(authentication(createAuth(customerId, "customer1", Role.CUSTOMER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
