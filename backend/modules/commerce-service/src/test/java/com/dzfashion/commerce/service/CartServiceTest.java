package com.dzfashion.commerce.service;

import com.dzfashion.commerce.client.CatalogClient;
import com.dzfashion.commerce.client.VariantSnapshot;
import com.dzfashion.commerce.dto.request.AddCartItemRequest;
import com.dzfashion.commerce.dto.response.CartResponse;
import com.dzfashion.commerce.entity.CartEntity;
import com.dzfashion.commerce.entity.CartItemEntity;
import com.dzfashion.commerce.exception.InvalidOperationException;
import com.dzfashion.commerce.repository.CartItemRepository;
import com.dzfashion.commerce.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private CatalogClient catalogClient;

    private CartService cartService;
    private UUID customerId;
    private UUID variantId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        cartService = new CartService(cartRepository, cartItemRepository, catalogClient);
        customerId = UUID.randomUUID();
        variantId = UUID.randomUUID();
        productId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Adding available variant adds item with catalog price snapshot")
    void addItemToCartSuccess() {
        VariantSnapshot variant = new VariantSnapshot(
                variantId, productId, "Silk Kurti", "DZF-SK-01", "M", "Rose",
                new BigDecimal("1200.00"), true, true
        );
        when(catalogClient.getVariant(variantId)).thenReturn(variant);

        CartEntity cart = new CartEntity(customerId);
        when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.save(any(CartItemEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(cartRepository.save(any(CartEntity.class))).thenAnswer(i -> i.getArgument(0));

        CartResponse response = cartService.addItemToCart(customerId, new AddCartItemRequest(variantId, 2));

        assertNotNull(response);
        assertEquals(1, response.items().size());
        assertEquals(2, response.totalItems());
        assertEquals(new BigDecimal("2400.00"), response.subtotal());
        assertFalse(response.priceConfirmationRequired());
    }

    @Test
    @DisplayName("Adding inactive or unpublished variant is rejected")
    void addInactiveVariantThrowsException() {
        VariantSnapshot inactiveVariant = new VariantSnapshot(
                variantId, productId, "Silk Kurti", "DZF-SK-01", "M", "Rose",
                new BigDecimal("1200.00"), false, true // inactive!
        );
        when(catalogClient.getVariant(variantId)).thenReturn(inactiveVariant);

        assertThrows(InvalidOperationException.class, () ->
                cartService.addItemToCart(customerId, new AddCartItemRequest(variantId, 1))
        );
    }

    @Test
    @DisplayName("Adding duplicate variant increments quantity")
    void addDuplicateVariantIncrementsQuantity() {
        VariantSnapshot variant = new VariantSnapshot(
                variantId, productId, "Silk Kurti", "DZF-SK-01", "M", "Rose",
                new BigDecimal("1200.00"), true, true
        );
        when(catalogClient.getVariant(variantId)).thenReturn(variant);

        CartEntity cart = new CartEntity(customerId);
        CartItemEntity existing = new CartItemEntity(productId, variantId, 1, new BigDecimal("1200.00"));
        cart.addItem(existing);

        when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.save(any(CartItemEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(cartRepository.save(any(CartEntity.class))).thenAnswer(i -> i.getArgument(0));

        CartResponse response = cartService.addItemToCart(customerId, new AddCartItemRequest(variantId, 2));

        assertEquals(1, response.items().size());
        assertEquals(3, response.totalItems(), "1 + 2 = 3 quantities");
        assertEquals(new BigDecimal("3600.00"), response.subtotal());
    }

    @Test
    @DisplayName("Price shift between addition and retrieval flags price confirmation required")
    void priceShiftDetected() {
        CartEntity cart = new CartEntity(customerId);
        // Added when price was ₹1200
        CartItemEntity item = new CartItemEntity(productId, variantId, 1, new BigDecimal("1200.00"));
        cart.addItem(item);

        when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.of(cart));

        // Now catalog price updated to ₹1500
        VariantSnapshot updatedVariant = new VariantSnapshot(
                variantId, productId, "Silk Kurti", "DZF-SK-01", "M", "Rose",
                new BigDecimal("1500.00"), true, true
        );
        when(catalogClient.getVariant(variantId)).thenReturn(updatedVariant);

        CartResponse response = cartService.getCustomerCart(customerId);

        assertTrue(response.priceConfirmationRequired(), "Must flag price confirmation required when catalog price shifts");
        assertTrue(response.items().get(0).priceChanged());
        assertEquals(new BigDecimal("1200.00"), response.items().get(0).priceSnapshot());
        assertEquals(new BigDecimal("1500.00"), response.items().get(0).currentPrice());
    }

    @Test
    @DisplayName("Clearing cart empties items")
    void clearCartRemovesAllItems() {
        CartEntity cart = new CartEntity(customerId);
        cart.addItem(new CartItemEntity(productId, variantId, 2, new BigDecimal("1000.00")));
        when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.of(cart));

        cartService.clearCart(customerId);

        assertTrue(cart.getItems().isEmpty());
        verify(cartRepository).save(cart);
    }
}
