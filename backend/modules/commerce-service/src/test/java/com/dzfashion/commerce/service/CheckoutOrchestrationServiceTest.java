package com.dzfashion.commerce.service;

import com.dzfashion.commerce.client.InventoryClient;
import com.dzfashion.commerce.client.ReservationResult;
import com.dzfashion.commerce.dto.request.ConfirmOrderRequest;
import com.dzfashion.commerce.dto.response.CartItemResponse;
import com.dzfashion.commerce.dto.response.CartResponse;
import com.dzfashion.commerce.dto.response.CheckoutPreviewResponse;
import com.dzfashion.commerce.dto.response.OrderResponse;
import com.dzfashion.commerce.entity.OrderEntity;
import com.dzfashion.commerce.enums.OrderStatus;
import com.dzfashion.commerce.enums.PaymentMethod;
import com.dzfashion.commerce.enums.PaymentStatus;
import com.dzfashion.commerce.exception.InsufficientStockException;
import com.dzfashion.commerce.exception.InvalidOperationException;
import com.dzfashion.commerce.exception.PriceConfirmationRequiredException;
import com.dzfashion.commerce.repository.OrderRepository;
import com.dzfashion.commerce.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckoutOrchestrationServiceTest {

    @Mock
    private CartService cartService;

    @Mock
    private CheckoutCalculationService calculationService;

    @Mock
    private CodPincodeService codPincodeService;

    @Mock
    private InventoryClient inventoryClient;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private CheckoutOrchestrationService orchestrationService;
    private UUID customerId;
    private UUID variantId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        orchestrationService = new CheckoutOrchestrationService(
                cartService,
                calculationService,
                codPincodeService,
                inventoryClient,
                orderRepository,
                outboxEventRepository,
                new ObjectMapper()
        );
        customerId = UUID.randomUUID();
        variantId = UUID.randomUUID();
        productId = UUID.randomUUID();
    }

    private ConfirmOrderRequest createValidRequest(PaymentMethod paymentMethod, boolean confirmPrices, String pincode) {
        return new ConfirmOrderRequest(
                paymentMethod,
                null,
                null,
                confirmPrices,
                "Aditi Sharma",
                "9876543210",
                "Flat 402, Lotus Residency",
                "Ring Road",
                "Surat",
                "Gujarat",
                pincode,
                "India"
        );
    }

    @Test
    @DisplayName("Empty cart checkout is rejected")
    void emptyCartCheckoutThrowsException() {
        when(cartService.getCustomerCart(customerId)).thenReturn(
                new CartResponse(UUID.randomUUID(), customerId, Collections.emptyList(), 0, BigDecimal.ZERO, false)
        );

        ConfirmOrderRequest request = createValidRequest(PaymentMethod.SIMULATED_ONLINE, false, "395001");

        assertThrows(InvalidOperationException.class, () ->
                orchestrationService.confirmOrder(customerId, request, "idemp-001")
        );
    }

    @Test
    @DisplayName("Unconfirmed price change prevents checkout")
    void unconfirmedPriceChangeRejectsCheckout() {
        CartItemResponse item = new CartItemResponse(
                UUID.randomUUID(), productId, variantId, "Silk Dress", "SKU-01", "M", "Red",
                1, new BigDecimal("1000.00"), new BigDecimal("1200.00"), true, true, new BigDecimal("1200.00")
        );
        when(cartService.getCustomerCart(customerId)).thenReturn(
                new CartResponse(UUID.randomUUID(), customerId, List.of(item), 1, new BigDecimal("1200.00"), true)
        );

        ConfirmOrderRequest request = createValidRequest(PaymentMethod.SIMULATED_ONLINE, false, "395001"); // confirmPriceChanges = false

        assertThrows(PriceConfirmationRequiredException.class, () ->
                orchestrationService.confirmOrder(customerId, request, "idemp-002")
        );
    }

    @Test
    @DisplayName("Unserviceable COD pincode is rejected")
    void unserviceableCodPincodeThrowsException() {
        CartItemResponse item = new CartItemResponse(
                UUID.randomUUID(), productId, variantId, "Silk Dress", "SKU-01", "M", "Red",
                1, new BigDecimal("1000.00"), new BigDecimal("1000.00"), false, true, new BigDecimal("1000.00")
        );
        when(cartService.getCustomerCart(customerId)).thenReturn(
                new CartResponse(UUID.randomUUID(), customerId, List.of(item), 1, new BigDecimal("1000.00"), false)
        );
        when(codPincodeService.isPincodeServiceable("999999")).thenReturn(false);

        ConfirmOrderRequest request = createValidRequest(PaymentMethod.COD, false, "999999");

        assertThrows(InvalidOperationException.class, () ->
                orchestrationService.confirmOrder(customerId, request, "idemp-003")
        );
    }

    @Test
    @DisplayName("Simulated online checkout confirms order, marks payment PAID, and reserves stock")
    void simulatedOnlineCheckoutSuccess() {
        CartItemResponse item = new CartItemResponse(
                UUID.randomUUID(), productId, variantId, "Silk Dress", "SKU-01", "M", "Red",
                2, new BigDecimal("1500.00"), new BigDecimal("1500.00"), false, true, new BigDecimal("3000.00")
        );
        CartResponse cart = new CartResponse(UUID.randomUUID(), customerId, List.of(item), 2, new BigDecimal("3000.00"), false);
        when(cartService.getCustomerCart(customerId)).thenReturn(cart);

        CheckoutPreviewResponse preview = new CheckoutPreviewResponse(
                new BigDecimal("3000.00"), 2, true, BigDecimal.ZERO, BigDecimal.ZERO, null,
                new BigDecimal("150.00"), new BigDecimal("5.00"), BigDecimal.ZERO, new BigDecimal("3150.00"),
                true, false, Collections.emptyList()
        );
        when(calculationService.calculate(any(), any(), any(), anyBoolean())).thenReturn(preview);

        UUID resId = UUID.randomUUID();
        when(inventoryClient.reserveStock(eq(variantId), eq(2), anyString(), anyString()))
                .thenReturn(ReservationResult.success(resId, variantId, 2));

        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(i -> {
            OrderEntity order = i.getArgument(0);
            order.setId(UUID.randomUUID());
            return order;
        });

        ConfirmOrderRequest request = createValidRequest(PaymentMethod.SIMULATED_ONLINE, false, "395001");
        OrderResponse response = orchestrationService.confirmOrder(customerId, request, "idemp-004");

        assertNotNull(response);
        assertEquals(OrderStatus.CONFIRMED, response.status());
        assertEquals(PaymentStatus.PAID, response.paymentStatus(), "Simulated online payment must be immediately PAID");
        assertEquals(new BigDecimal("3150.00"), response.finalPayableAmount());

        verify(cartService).clearCart(customerId);
        verify(outboxEventRepository).save(any());
    }

    @Test
    @DisplayName("Failed stock reservation creates no order and releases prior reservations")
    void failedReservationAbortsOrder() {
        CartItemResponse item1 = new CartItemResponse(
                UUID.randomUUID(), productId, variantId, "Item 1", "SKU-01", "M", "Red",
                1, new BigDecimal("1000.00"), new BigDecimal("1000.00"), false, true, new BigDecimal("1000.00")
        );
        UUID variant2 = UUID.randomUUID();
        CartItemResponse item2 = new CartItemResponse(
                UUID.randomUUID(), UUID.randomUUID(), variant2, "Item 2", "SKU-02", "L", "Blue",
                1, new BigDecimal("1000.00"), new BigDecimal("1000.00"), false, true, new BigDecimal("1000.00")
        );
        CartResponse cart = new CartResponse(UUID.randomUUID(), customerId, List.of(item1, item2), 2, new BigDecimal("2000.00"), false);
        when(cartService.getCustomerCart(customerId)).thenReturn(cart);

        CheckoutPreviewResponse preview = new CheckoutPreviewResponse(
                new BigDecimal("2000.00"), 2, false, new BigDecimal("220.00"), BigDecimal.ZERO, null,
                new BigDecimal("100.00"), new BigDecimal("5.00"), BigDecimal.ZERO, new BigDecimal("2320.00"),
                true, false, Collections.emptyList()
        );
        when(calculationService.calculate(any(), any(), any(), anyBoolean())).thenReturn(preview);

        UUID res1 = UUID.randomUUID();
        when(inventoryClient.reserveStock(eq(variantId), eq(1), anyString(), anyString()))
                .thenReturn(ReservationResult.success(res1, variantId, 1));
        when(inventoryClient.reserveStock(eq(variant2), eq(1), anyString(), anyString()))
                .thenReturn(ReservationResult.failure(variant2, 1, "Out of stock"));

        ConfirmOrderRequest request = createValidRequest(PaymentMethod.SIMULATED_ONLINE, false, "395001");

        assertThrows(InsufficientStockException.class, () ->
                orchestrationService.confirmOrder(customerId, request, "idemp-005")
        );

        // Verify compensation: item 1 was released
        verify(inventoryClient).releaseStock(eq(res1), anyString());
        // Verify order was NEVER saved
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Idempotent checkout returns existing order without duplicate charge or reservation")
    void idempotentCheckoutReturnsExistingOrder() {
        OrderEntity existingOrder = new OrderEntity();
        existingOrder.setId(UUID.randomUUID());
        existingOrder.setOrderNumber("DZF-20261115-EXIST1");
        existingOrder.setIdempotencyKey("idemp-duplicate");
        existingOrder.setCustomerId(customerId);
        existingOrder.setStatus(OrderStatus.CONFIRMED);
        existingOrder.setPaymentMethod(PaymentMethod.SIMULATED_ONLINE);
        existingOrder.setPaymentStatus(PaymentStatus.PAID);
        existingOrder.setSubtotalBeforeDiscountAndTax(new BigDecimal("1000.00"));
        existingOrder.setFinalPayableAmount(new BigDecimal("1050.00"));
        existingOrder.setRecipientName("Aditi");
        existingOrder.setRecipientPhone("9876543210");
        existingOrder.setShippingAddressLine1("Line 1");
        existingOrder.setShippingCity("Surat");
        existingOrder.setShippingState("Gujarat");
        existingOrder.setShippingPostalCode("395001");

        when(orderRepository.findByIdempotencyKey("idemp-duplicate")).thenReturn(Optional.of(existingOrder));

        ConfirmOrderRequest request = createValidRequest(PaymentMethod.SIMULATED_ONLINE, false, "395001");
        OrderResponse response = orchestrationService.confirmOrder(customerId, request, "idemp-duplicate");

        assertEquals("DZF-20261115-EXIST1", response.orderNumber());
        verify(inventoryClient, never()).reserveStock(any(), anyInt(), anyString(), anyString());
        verify(orderRepository, never()).save(any());
    }
}
