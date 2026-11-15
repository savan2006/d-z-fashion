package com.dzfashion.commerce.controller;

import com.dzfashion.commerce.dto.request.AddCartItemRequest;
import com.dzfashion.commerce.dto.request.UpdateCartItemRequest;
import com.dzfashion.commerce.dto.response.ActionResponse;
import com.dzfashion.commerce.dto.response.CartResponse;
import com.dzfashion.commerce.security.UserPrincipal;
import com.dzfashion.commerce.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cart")
@PreAuthorize("hasRole('CUSTOMER')")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<CartResponse> getCart(@AuthenticationPrincipal UserPrincipal principal) {
        UUID customerId = getEffectiveUserId(principal);
        return ResponseEntity.ok(cartService.getCustomerCart(customerId));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(@AuthenticationPrincipal UserPrincipal principal,
                                                @Valid @RequestBody AddCartItemRequest request) {
        UUID customerId = getEffectiveUserId(principal);
        return ResponseEntity.ok(cartService.addItemToCart(customerId, request));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> updateItemQuantity(@AuthenticationPrincipal UserPrincipal principal,
                                                          @PathVariable UUID itemId,
                                                          @Valid @RequestBody UpdateCartItemRequest request) {
        UUID customerId = getEffectiveUserId(principal);
        return ResponseEntity.ok(cartService.updateItemQuantity(customerId, itemId, request.quantity()));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> removeItem(@AuthenticationPrincipal UserPrincipal principal,
                                                   @PathVariable UUID itemId) {
        UUID customerId = getEffectiveUserId(principal);
        return ResponseEntity.ok(cartService.removeItemFromCart(customerId, itemId));
    }

    @DeleteMapping
    public ResponseEntity<ActionResponse> clearCart(@AuthenticationPrincipal UserPrincipal principal) {
        UUID customerId = getEffectiveUserId(principal);
        cartService.clearCart(customerId);
        return ResponseEntity.ok(ActionResponse.ok("Cart cleared successfully"));
    }

    private UUID getEffectiveUserId(UserPrincipal principal) {
        return principal != null ? principal.getId() : UUID.randomUUID();
    }
}
