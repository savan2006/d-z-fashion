package com.dzfashion.commerce.service;

import com.dzfashion.commerce.client.InventoryClient;
import com.dzfashion.commerce.dto.request.CancelOrderRequest;
import com.dzfashion.commerce.dto.request.ReassignOrderRequest;
import com.dzfashion.commerce.dto.request.ReportShortageRequest;
import com.dzfashion.commerce.dto.request.ResolveShortageRequest;
import com.dzfashion.commerce.dto.response.OrderResponse;
import com.dzfashion.commerce.dto.response.ShortageIncidentResponse;
import com.dzfashion.commerce.entity.OrderEntity;
import com.dzfashion.commerce.entity.OrderItemEntity;
import com.dzfashion.commerce.entity.OrderShortageIncidentEntity;
import com.dzfashion.commerce.enums.OrderStatus;
import com.dzfashion.commerce.enums.PaymentMethod;
import com.dzfashion.commerce.enums.PaymentStatus;
import com.dzfashion.commerce.enums.Role;
import com.dzfashion.commerce.enums.ShortageIncidentStatus;
import com.dzfashion.commerce.exception.InvalidOperationException;
import com.dzfashion.commerce.exception.ResourceConflictException;
import com.dzfashion.commerce.repository.OrderClaimHistoryRepository;
import com.dzfashion.commerce.repository.OrderRepository;
import com.dzfashion.commerce.repository.OrderShortageIncidentRepository;
import com.dzfashion.commerce.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderLifecycleServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderClaimHistoryRepository claimHistoryRepository;

    @Mock
    private OrderShortageIncidentRepository shortageIncidentRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private InventoryClient inventoryClient;

    @Mock
    private CheckoutOrchestrationService checkoutOrchestrationService;

    private OrderLifecycleService lifecycleService;
    private UUID customerId;
    private UUID managerId;
    private UUID ownerId;
    private String orderNumber;

    @BeforeEach
    void setUp() {
        lifecycleService = new OrderLifecycleService(
                orderRepository,
                claimHistoryRepository,
                shortageIncidentRepository,
                outboxEventRepository,
                inventoryClient,
                checkoutOrchestrationService,
                new ObjectMapper()
        );
        customerId = UUID.randomUUID();
        managerId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        orderNumber = "DZF-20261115-ABC12";
    }

    private OrderEntity createTestOrder(OrderStatus status) {
        OrderEntity order = new OrderEntity();
        order.setId(UUID.randomUUID());
        order.setOrderNumber(orderNumber);
        order.setCustomerId(customerId);
        order.setStatus(status);
        order.setPaymentMethod(PaymentMethod.SIMULATED_ONLINE);
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setSubtotalBeforeDiscountAndTax(new BigDecimal("2500.00"));
        order.setFinalPayableAmount(new BigDecimal("2625.00"));
        order.setRecipientName("Pooja");
        order.setRecipientPhone("9876543210");
        order.setShippingAddressLine1("Line 1");
        order.setShippingCity("Surat");
        order.setShippingState("Gujarat");
        order.setShippingPostalCode("395001");

        OrderItemEntity item = new OrderItemEntity(
                UUID.randomUUID(), UUID.randomUUID(), "Designer Kurti", "DZF-KURTI-01",
                "M", "Pink", 1, new BigDecimal("2500.00"), new BigDecimal("2500.00")
        );
        order.addItem(item);
        return order;
    }

    @Test
    @DisplayName("Customer can view their own order; another customer is denied")
    void customerOrderAccessControl() {
        OrderEntity order = createTestOrder(OrderStatus.CONFIRMED);
        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(order));

        // Owning customer succeeds
        lifecycleService.getOrderDetails(customerId, Role.CUSTOMER, orderNumber);
        verify(checkoutOrchestrationService).mapToOrderResponse(order);

        // Different customer is denied
        UUID otherCustomer = UUID.randomUUID();
        assertThrows(AccessDeniedException.class, () ->
                lifecycleService.getOrderDetails(otherCustomer, Role.CUSTOMER, orderNumber)
        );
    }

    @Test
    @DisplayName("Customer self-cancellation permitted pre-dispatch and releases stock")
    void customerSelfCancellationPreDispatch() {
        OrderEntity order = createTestOrder(OrderStatus.CONFIRMED);
        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(i -> i.getArgument(0));

        lifecycleService.cancelOrder(customerId, Role.CUSTOMER, orderNumber, new CancelOrderRequest("Changed my mind"));

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(customerId, order.getCancelledBy());
        assertEquals(PaymentStatus.REFUNDED, order.getPaymentStatus());
        assertNotNull(order.getCancelledAt());

        // Verify stock release was requested
        verify(inventoryClient).releaseStock(eq(order.getItems().get(0).getVariantId()), anyString());
        verify(outboxEventRepository).save(any());
    }

    @Test
    @DisplayName("Customer cancellation is strictly denied after dispatch")
    void cancelAfterDispatchDenied() {
        OrderEntity order = createTestOrder(OrderStatus.SHIPPED);
        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(order));

        assertThrows(InvalidOperationException.class, () ->
                lifecycleService.cancelOrder(customerId, Role.CUSTOMER, orderNumber, new CancelOrderRequest("Too late"))
        );
    }

    @Test
    @DisplayName("Manager is strictly prohibited from cancelling orders")
    void managerCannotCancelOrder() {
        assertThrows(AccessDeniedException.class, () ->
                lifecycleService.cancelOrder(managerId, Role.MANAGER, orderNumber, new CancelOrderRequest("Manager cancel"))
        );
    }

    @Test
    @DisplayName("Manager atomic claim succeeds when unclaimed")
    void managerClaimOrderSuccess() {
        OrderEntity order = createTestOrder(OrderStatus.CONFIRMED);
        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(i -> i.getArgument(0));

        lifecycleService.claimOrder(managerId, orderNumber);

        assertEquals(managerId, order.getClaimedBy());
        assertEquals(OrderStatus.PROCESSING, order.getStatus(), "Status transitions to PROCESSING upon claim");
        assertNotNull(order.getClaimedAt());
        verify(claimHistoryRepository).save(any());
    }

    @Test
    @DisplayName("Claim race: already claimed order rejects second manager with 409 Conflict")
    void claimRaceRejectsSecondManager() {
        OrderEntity order = createTestOrder(OrderStatus.PROCESSING);
        order.setClaimedBy(managerId); // already claimed by manager 1
        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(order));

        UUID secondManager = UUID.randomUUID();
        assertThrows(ResourceConflictException.class, () ->
                lifecycleService.claimOrder(secondManager, orderNumber)
        );
    }

    @Test
    @DisplayName("Owner can reassign order to another manager")
    void ownerReassignOrderSuccess() {
        OrderEntity order = createTestOrder(OrderStatus.PROCESSING);
        order.setClaimedBy(managerId);
        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(i -> i.getArgument(0));

        UUID newManager = UUID.randomUUID();
        lifecycleService.reassignOrder(ownerId, orderNumber, new ReassignOrderRequest(newManager, "Shift handover"));

        assertEquals(newManager, order.getClaimedBy());
        verify(claimHistoryRepository).save(any());
    }

    @Test
    @DisplayName("Dispatch order transitions status to SHIPPED and commits stock")
    void dispatchOrderSuccess() {
        OrderEntity order = createTestOrder(OrderStatus.PROCESSING);
        order.setClaimedBy(managerId);
        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(i -> i.getArgument(0));

        lifecycleService.dispatchOrder(managerId, orderNumber);

        assertEquals(OrderStatus.SHIPPED, order.getStatus());
        assertNotNull(order.getDispatchedAt());
        verify(inventoryClient).commitStock(order.getItems().get(0).getVariantId());
    }

    @Test
    @DisplayName("Deliver order transitions to DELIVERED and marks COD payment PAID")
    void deliverOrderSuccess() {
        OrderEntity order = createTestOrder(OrderStatus.SHIPPED);
        order.setPaymentMethod(PaymentMethod.COD);
        order.setPaymentStatus(PaymentStatus.PENDING);
        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(i -> i.getArgument(0));

        lifecycleService.deliverOrder(managerId, orderNumber);

        assertEquals(OrderStatus.DELIVERED, order.getStatus());
        assertEquals(PaymentStatus.PAID, order.getPaymentStatus(), "COD payment is marked PAID upon delivery");
        assertNotNull(order.getDeliveredAt());
    }

    @Test
    @DisplayName("Reporting shortage creates incident for Owner and does NOT auto-cancel order")
    void reportShortageIncidentDoesNotAutoCancel() {
        OrderEntity order = createTestOrder(OrderStatus.PROCESSING);
        order.setClaimedBy(managerId);
        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(order));

        UUID variantId = order.getItems().get(0).getVariantId();
        ReportShortageRequest request = new ReportShortageRequest(variantId, 1, 0, "Item torn on warehouse shelf");

        when(shortageIncidentRepository.save(any(OrderShortageIncidentEntity.class))).thenAnswer(i -> {
            OrderShortageIncidentEntity e = i.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        ShortageIncidentResponse response = lifecycleService.reportShortageIncident(managerId, orderNumber, request);

        assertNotNull(response);
        assertEquals(ShortageIncidentStatus.REPORTED, response.status());
        assertNotEquals(OrderStatus.CANCELLED, order.getStatus(), "Order must NOT be auto-cancelled on shortage");
        verify(shortageIncidentRepository).save(any());
        verify(claimHistoryRepository).save(any());
    }

    @Test
    @DisplayName("Owner resolves shortage incident")
    void ownerResolvesShortageIncident() {
        OrderEntity order = createTestOrder(OrderStatus.PROCESSING);
        OrderShortageIncidentEntity incident = new OrderShortageIncidentEntity(
                "SHT-20261115-11111", order.getId(), UUID.randomUUID(), managerId, 1, 0, "Missing piece"
        );
        when(shortageIncidentRepository.findByIncidentNumber("SHT-20261115-11111")).thenReturn(Optional.of(incident));
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(shortageIncidentRepository.save(any(OrderShortageIncidentEntity.class))).thenAnswer(i -> i.getArgument(0));

        ShortageIncidentResponse response = lifecycleService.resolveShortageIncident(
                ownerId, "SHT-20261115-11111", new ResolveShortageRequest("Customer contacted, agreed on alternative colour")
        );

        assertEquals(ShortageIncidentStatus.RESOLVED, response.status());
        assertEquals("Customer contacted, agreed on alternative colour", response.resolutionNotes());
        assertEquals(ownerId, response.resolvedBy());
    }
}
