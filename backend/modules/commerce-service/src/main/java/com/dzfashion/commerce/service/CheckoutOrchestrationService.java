package com.dzfashion.commerce.service;

import com.dzfashion.commerce.client.InventoryClient;
import com.dzfashion.commerce.client.ReservationResult;
import com.dzfashion.commerce.dto.request.CheckoutPreviewRequest;
import com.dzfashion.commerce.dto.request.ConfirmOrderRequest;
import com.dzfashion.commerce.dto.response.CartItemResponse;
import com.dzfashion.commerce.dto.response.CartResponse;
import com.dzfashion.commerce.dto.response.CheckoutPreviewResponse;
import com.dzfashion.commerce.dto.response.OrderItemResponse;
import com.dzfashion.commerce.dto.response.OrderResponse;
import com.dzfashion.commerce.entity.OrderEntity;
import com.dzfashion.commerce.entity.OrderItemEntity;
import com.dzfashion.commerce.entity.OutboxEventEntity;
import com.dzfashion.commerce.enums.OrderStatus;
import com.dzfashion.commerce.enums.PaymentMethod;
import com.dzfashion.commerce.enums.PaymentStatus;
import com.dzfashion.commerce.exception.InsufficientStockException;
import com.dzfashion.commerce.exception.InvalidOperationException;
import com.dzfashion.commerce.exception.PriceConfirmationRequiredException;
import com.dzfashion.commerce.repository.OrderRepository;
import com.dzfashion.commerce.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CheckoutOrchestrationService {

    private final CartService cartService;
    private final CheckoutCalculationService calculationService;
    private final CodPincodeService codPincodeService;
    private final InventoryClient inventoryClient;
    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public CheckoutOrchestrationService(CartService cartService,
                                        CheckoutCalculationService calculationService,
                                        CodPincodeService codPincodeService,
                                        InventoryClient inventoryClient,
                                        OrderRepository orderRepository,
                                        OutboxEventRepository outboxEventRepository,
                                        ObjectMapper objectMapper) {
        this.cartService = cartService;
        this.calculationService = calculationService;
        this.codPincodeService = codPincodeService;
        this.inventoryClient = inventoryClient;
        this.orderRepository = orderRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public CheckoutPreviewResponse previewCheckout(UUID customerId, CheckoutPreviewRequest request) {
        CartResponse cart = cartService.getCustomerCart(customerId);
        if (cart.items().isEmpty()) {
            throw new InvalidOperationException("Cannot preview checkout for an empty cart");
        }

        boolean codServiceable = true;
        if (request.paymentMethod() == PaymentMethod.COD && request.postalCode() != null && !request.postalCode().isBlank()) {
            codServiceable = codPincodeService.isPincodeServiceable(request.postalCode());
        }

        return calculationService.calculate(
                cart,
                request.couponCode(),
                request.storeCreditToUse(),
                codServiceable
        );
    }

    @Transactional
    public OrderResponse confirmOrder(UUID customerId, ConfirmOrderRequest request, String idempotencyKey) {
        // 1. Idempotency check
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existingOrder = orderRepository.findByIdempotencyKey(idempotencyKey.trim());
            if (existingOrder.isPresent()) {
                return mapToOrderResponse(existingOrder.get());
            }
        }

        // 2. Cart verification
        CartResponse cart = cartService.getCustomerCart(customerId);
        if (cart.items().isEmpty()) {
            throw new InvalidOperationException("Cannot checkout with an empty cart");
        }

        // 3. Price confirmation check
        if (cart.priceConfirmationRequired() && !request.confirmPriceChanges()) {
            throw new PriceConfirmationRequiredException("One or more items in the cart have updated prices. Explicit confirmation is required.");
        }

        // 4. Variant availability check
        for (CartItemResponse item : cart.items()) {
            if (!item.available()) {
                throw new InvalidOperationException("Cannot checkout because product variant " + item.productName() + " (" + item.sku() + ") is unavailable or unpublished");
            }
        }

        // 5. Payment method & COD pincode verification
        boolean isCod = request.paymentMethod() == PaymentMethod.COD;
        if (isCod) {
            boolean serviceable = codPincodeService.isPincodeServiceable(request.shippingPostalCode());
            if (!serviceable) {
                throw new InvalidOperationException("Cash on Delivery (COD) is not serviceable for postal code: " + request.shippingPostalCode());
            }
        }

        // 6. Calculate authoritative totals
        CheckoutPreviewResponse calculation = calculationService.calculate(
                cart,
                request.couponCode(),
                request.storeCreditToUse(),
                isCod
        );

        // 7. Generate order number
        String datePrefix = LocalDate.now(ZoneId.of("Asia/Kolkata")).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String orderNumber = "DZF-" + datePrefix + "-" + randomSuffix;

        // 8. Atomic inventory reservation handshake with compensation
        List<UUID> successfulReservationIds = new ArrayList<>();
        try {
            for (CartItemResponse item : cart.items()) {
                String itemKey = (idempotencyKey != null ? idempotencyKey : orderNumber) + "-" + item.variantId();
                ReservationResult result = inventoryClient.reserveStock(item.variantId(), item.quantity(), orderNumber, itemKey);
                if (!result.success()) {
                    throw new InsufficientStockException("Insufficient stock to reserve variant " + item.productName() + " (" + item.sku() + "): " + result.message());
                }
                successfulReservationIds.add(result.reservationId());
            }
        } catch (Exception e) {
            // Compensate: release all reserved items on partial failure
            for (UUID resId : successfulReservationIds) {
                try {
                    inventoryClient.releaseStock(resId, "Checkout aborted due to stock reservation failure on subsequent item");
                } catch (Exception releaseEx) {
                    // Log and proceed with compensation
                }
            }
            if (e instanceof InsufficientStockException) {
                throw (InsufficientStockException) e;
            }
            throw new InsufficientStockException("Failed to reserve stock during checkout: " + e.getMessage());
        }

        // 9. Persist Order and Order Items
        OrderEntity order = new OrderEntity();
        order.setOrderNumber(orderNumber);
        order.setIdempotencyKey(idempotencyKey != null && !idempotencyKey.isBlank() ? idempotencyKey.trim() : null);
        order.setCustomerId(customerId);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setPaymentMethod(request.paymentMethod());
        order.setPaymentStatus(isCod ? PaymentStatus.PENDING : PaymentStatus.PAID);
        order.setSubtotalBeforeDiscountAndTax(calculation.subtotalBeforeDiscountAndTax());
        order.setPromotionalDiscountAmount(calculation.promotionalDiscountAmount());
        order.setGstAmount(calculation.gstAmount());
        order.setDeliveryCharge(calculation.deliveryCharge());
        order.setStoreCreditUsed(calculation.storeCreditUsed());
        order.setFinalPayableAmount(calculation.finalPayableAmount());

        order.setRecipientName(request.recipientName());
        order.setRecipientPhone(request.recipientPhone());
        order.setShippingAddressLine1(request.shippingAddressLine1());
        order.setShippingAddressLine2(request.shippingAddressLine2());
        order.setShippingCity(request.shippingCity());
        order.setShippingState(request.shippingState());
        order.setShippingPostalCode(request.shippingPostalCode());
        order.setShippingCountry(request.getEffectiveCountry());

        for (CartItemResponse item : cart.items()) {
            OrderItemEntity orderItem = new OrderItemEntity(
                    item.productId(),
                    item.variantId(),
                    item.productName(),
                    item.sku(),
                    item.size(),
                    item.colour(),
                    item.quantity(),
                    item.currentPrice(),
                    item.itemTotal()
            );
            order.addItem(orderItem);
        }

        OrderEntity savedOrder = orderRepository.save(order);

        // 10. Clear Customer Cart
        cartService.clearCart(customerId);

        // 11. Transactional Outbox Event
        try {
            String payload = objectMapper.writeValueAsString(new OrderCreatedPayload(
                    savedOrder.getId(),
                    savedOrder.getOrderNumber(),
                    savedOrder.getCustomerId(),
                    savedOrder.getFinalPayableAmount(),
                    savedOrder.getPaymentMethod().name()
            ));
            outboxEventRepository.save(new OutboxEventEntity("ORDER", savedOrder.getId().toString(), "ORDER_CONFIRMED", payload));
        } catch (Exception e) {
            // Outbox serialization exception
        }

        return mapToOrderResponse(savedOrder);
    }

    public OrderResponse mapToOrderResponse(OrderEntity order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(i -> new OrderItemResponse(
                        i.getId(),
                        i.getProductId(),
                        i.getVariantId(),
                        i.getProductName(),
                        i.getSku(),
                        i.getSize(),
                        i.getColour(),
                        i.getQuantity(),
                        i.getUnitPrice(),
                        i.getTotalPrice()
                ))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getIdempotencyKey(),
                order.getCustomerId(),
                order.getStatus(),
                order.getPaymentMethod(),
                order.getPaymentStatus(),
                order.getClaimedBy(),
                order.getClaimedAt(),
                order.getSubtotalBeforeDiscountAndTax(),
                order.getPromotionalDiscountAmount(),
                order.getAppliedPromotionId(),
                order.getGstAmount(),
                order.getDeliveryCharge(),
                order.getStoreCreditUsed(),
                order.getFinalPayableAmount(),
                order.getRecipientName(),
                order.getRecipientPhone(),
                order.getShippingAddressLine1(),
                order.getShippingAddressLine2(),
                order.getShippingCity(),
                order.getShippingState(),
                order.getShippingPostalCode(),
                order.getShippingCountry(),
                order.getConfirmedAt(),
                order.getDispatchedAt(),
                order.getDeliveredAt(),
                order.getCancelledAt(),
                order.getCancelledBy(),
                order.getCancellationReason(),
                itemResponses
        );
    }

    public record OrderCreatedPayload(UUID orderId, String orderNumber, UUID customerId, java.math.BigDecimal amount, String paymentMethod) {}
}
