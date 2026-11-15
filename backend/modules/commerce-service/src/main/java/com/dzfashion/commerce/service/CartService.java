package com.dzfashion.commerce.service;

import com.dzfashion.commerce.client.CatalogClient;
import com.dzfashion.commerce.client.VariantSnapshot;
import com.dzfashion.commerce.dto.request.AddCartItemRequest;
import com.dzfashion.commerce.dto.response.CartItemResponse;
import com.dzfashion.commerce.dto.response.CartResponse;
import com.dzfashion.commerce.entity.CartEntity;
import com.dzfashion.commerce.entity.CartItemEntity;
import com.dzfashion.commerce.exception.InvalidOperationException;
import com.dzfashion.commerce.exception.ResourceNotFoundException;
import com.dzfashion.commerce.repository.CartItemRepository;
import com.dzfashion.commerce.repository.CartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CatalogClient catalogClient;

    public CartService(CartRepository cartRepository,
                       CartItemRepository cartItemRepository,
                       CatalogClient catalogClient) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.catalogClient = catalogClient;
    }

    @Transactional
    public CartEntity getOrCreateCart(UUID customerId) {
        return cartRepository.findByCustomerId(customerId)
                .orElseGet(() -> cartRepository.save(new CartEntity(customerId)));
    }

    @Transactional(readOnly = true)
    public CartResponse getCustomerCart(UUID customerId) {
        CartEntity cart = cartRepository.findByCustomerId(customerId)
                .orElseGet(() -> new CartEntity(customerId));

        return buildCartResponse(cart);
    }

    @Transactional
    public CartResponse addItemToCart(UUID customerId, AddCartItemRequest request) {
        if (request.quantity() <= 0) {
            throw new InvalidOperationException("Item quantity must be strictly greater than zero");
        }

        VariantSnapshot variant = catalogClient.getVariant(request.variantId());
        if (!variant.active() || !variant.published()) {
            throw new InvalidOperationException("Cannot add variant " + request.variantId() + " to cart because it is unavailable or unpublished");
        }

        CartEntity cart = getOrCreateCart(customerId);

        Optional<CartItemEntity> existingItem = cart.getItems().stream()
                .filter(i -> i.getVariantId().equals(request.variantId()))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItemEntity item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.quantity());
            cartItemRepository.save(item);
        } else {
            CartItemEntity newItem = new CartItemEntity(
                    variant.productId(),
                    variant.variantId(),
                    request.quantity(),
                    variant.price()
            );
            cart.addItem(newItem);
            cartItemRepository.save(newItem);
        }

        cartRepository.save(cart);
        return buildCartResponse(cart);
    }

    @Transactional
    public CartResponse updateItemQuantity(UUID customerId, UUID itemId, int quantity) {
        if (quantity <= 0) {
            throw new InvalidOperationException("Item quantity must be strictly greater than zero");
        }

        CartEntity cart = getOrCreateCart(customerId);
        CartItemEntity item = cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cart item " + itemId + " not found in customer cart"));

        item.setQuantity(quantity);
        cartItemRepository.save(item);
        cartRepository.save(cart);

        return buildCartResponse(cart);
    }

    @Transactional
    public CartResponse removeItemFromCart(UUID customerId, UUID itemId) {
        CartEntity cart = getOrCreateCart(customerId);
        CartItemEntity item = cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cart item " + itemId + " not found in customer cart"));

        cart.removeItem(item);
        cartItemRepository.delete(item);
        cartRepository.save(cart);

        return buildCartResponse(cart);
    }

    @Transactional
    public void clearCart(UUID customerId) {
        CartEntity cart = getOrCreateCart(customerId);
        cart.clearItems();
        cartRepository.save(cart);
    }

    public CartResponse buildCartResponse(CartEntity cart) {
        List<CartItemResponse> itemResponses = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        int totalPhysicalUnits = 0;
        boolean priceConfirmationRequired = false;

        for (CartItemEntity item : cart.getItems()) {
            VariantSnapshot variant;
            try {
                variant = catalogClient.getVariant(item.getVariantId());
            } catch (Exception e) {
                // If variant no longer found, mark unavailable
                variant = new VariantSnapshot(
                        item.getVariantId(),
                        item.getProductId(),
                        "Unknown Product",
                        "UNAVAILABLE",
                        "-",
                        "-",
                        item.getPriceSnapshot(),
                        false,
                        false
                );
            }

            BigDecimal currentPrice = variant.price();
            boolean priceChanged = item.getPriceSnapshot().compareTo(currentPrice) != 0;
            if (priceChanged) {
                priceConfirmationRequired = true;
            }

            BigDecimal lineTotal = currentPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            subtotal = subtotal.add(lineTotal);
            totalPhysicalUnits += item.getQuantity();

            itemResponses.add(new CartItemResponse(
                    item.getId(),
                    item.getProductId(),
                    item.getVariantId(),
                    variant.productName(),
                    variant.sku(),
                    variant.size(),
                    variant.colour(),
                    item.getQuantity(),
                    item.getPriceSnapshot(),
                    currentPrice,
                    priceChanged,
                    variant.active() && variant.published(),
                    lineTotal
            ));
        }

        return new CartResponse(
                cart.getId(),
                cart.getCustomerId(),
                itemResponses,
                totalPhysicalUnits,
                subtotal,
                priceConfirmationRequired
        );
    }
}
