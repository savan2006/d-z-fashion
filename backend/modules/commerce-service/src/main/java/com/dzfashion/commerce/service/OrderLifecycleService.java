package com.dzfashion.commerce.service;

import com.dzfashion.commerce.client.InventoryClient;
import com.dzfashion.commerce.dto.request.CancelOrderRequest;
import com.dzfashion.commerce.dto.request.ReassignOrderRequest;
import com.dzfashion.commerce.dto.request.ReportShortageRequest;
import com.dzfashion.commerce.dto.request.ResolveShortageRequest;
import com.dzfashion.commerce.dto.response.OrderResponse;
import com.dzfashion.commerce.dto.response.OrderSummaryResponse;
import com.dzfashion.commerce.dto.response.PageResponse;
import com.dzfashion.commerce.dto.response.ShortageIncidentResponse;
import com.dzfashion.commerce.entity.OrderClaimHistoryEntity;
import com.dzfashion.commerce.entity.OrderEntity;
import com.dzfashion.commerce.entity.OrderItemEntity;
import com.dzfashion.commerce.entity.OrderShortageIncidentEntity;
import com.dzfashion.commerce.entity.OutboxEventEntity;
import com.dzfashion.commerce.enums.OrderClaimAction;
import com.dzfashion.commerce.enums.OrderStatus;
import com.dzfashion.commerce.enums.PaymentMethod;
import com.dzfashion.commerce.enums.PaymentStatus;
import com.dzfashion.commerce.enums.Role;
import com.dzfashion.commerce.enums.ShortageIncidentStatus;
import com.dzfashion.commerce.exception.InvalidOperationException;
import com.dzfashion.commerce.exception.ResourceConflictException;
import com.dzfashion.commerce.exception.ResourceNotFoundException;
import com.dzfashion.commerce.repository.OrderClaimHistoryRepository;
import com.dzfashion.commerce.repository.OrderRepository;
import com.dzfashion.commerce.repository.OrderShortageIncidentRepository;
import com.dzfashion.commerce.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class OrderLifecycleService {

    private final OrderRepository orderRepository;
    private final OrderClaimHistoryRepository claimHistoryRepository;
    private final OrderShortageIncidentRepository shortageIncidentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final InventoryClient inventoryClient;
    private final CheckoutOrchestrationService checkoutOrchestrationService;
    private final ObjectMapper objectMapper;

    public OrderLifecycleService(OrderRepository orderRepository,
                                 OrderClaimHistoryRepository claimHistoryRepository,
                                 OrderShortageIncidentRepository shortageIncidentRepository,
                                 OutboxEventRepository outboxEventRepository,
                                 InventoryClient inventoryClient,
                                 CheckoutOrchestrationService checkoutOrchestrationService,
                                 ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.claimHistoryRepository = claimHistoryRepository;
        this.shortageIncidentRepository = shortageIncidentRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.inventoryClient = inventoryClient;
        this.checkoutOrchestrationService = checkoutOrchestrationService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> getCustomerOrders(UUID customerId, Pageable pageable) {
        Page<OrderEntity> page = orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable);
        List<OrderSummaryResponse> summaries = page.getContent().stream()
                .map(this::mapToSummary)
                .toList();

        return new PageResponse<>(
                summaries,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderDetails(UUID actorId, Role actorRole, String orderNumber) {
        OrderEntity order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order " + orderNumber + " not found"));

        if (actorRole == Role.CUSTOMER && !order.getCustomerId().equals(actorId)) {
            throw new AccessDeniedException("You are not authorized to view this order");
        }

        return checkoutOrchestrationService.mapToOrderResponse(order);
    }

    @Transactional
    public OrderResponse cancelOrder(UUID actorId, Role actorRole, String orderNumber, CancelOrderRequest request) {
        // Rule: Managers NEVER cancel an order
        if (actorRole == Role.MANAGER) {
            throw new AccessDeniedException("Managers are not permitted to cancel orders");
        }

        OrderEntity order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order " + orderNumber + " not found"));

        // Customer self-cancellation ownership check
        if (actorRole == Role.CUSTOMER && !order.getCustomerId().equals(actorId)) {
            throw new AccessDeniedException("You can only cancel your own orders");
        }

        // Rule: Pre-dispatch only. After dispatch, cancellation is strictly disabled.
        if (!order.getStatus().isCancelableByCustomer()) {
            throw new InvalidOperationException("Order cannot be cancelled because it is in state: " + order.getStatus() + ". Cancellation is permitted only before dispatch.");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(Instant.now());
        order.setCancelledBy(actorId);
        order.setCancellationReason(request.reason());

        // For simulated payment, record virtual payment reversal state
        if (order.getPaymentMethod() == PaymentMethod.SIMULATED_ONLINE) {
            order.setPaymentStatus(PaymentStatus.REFUNDED);
        }

        // Release stock reservations idempotently
        for (OrderItemEntity item : order.getItems()) {
            try {
                // In a distributed saga, the reservation ID or variant reference is released
                inventoryClient.releaseStock(item.getVariantId(), "Order cancelled: " + request.reason());
            } catch (Exception e) {
                // Ignore idempotent double-release
            }
        }

        OrderEntity saved = orderRepository.save(order);

        // Transactional outbox event
        saveOutboxEvent("ORDER", saved.getId().toString(), "ORDER_CANCELLED", new OrderCancelledPayload(saved.getId(), saved.getOrderNumber(), request.reason(), actorId));

        return checkoutOrchestrationService.mapToOrderResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> getManagerOrderQueue(Pageable pageable) {
        Page<OrderEntity> page = orderRepository.findByStatusInOrderByConfirmedAtAsc(
                List.of(OrderStatus.CONFIRMED, OrderStatus.PROCESSING),
                pageable
        );

        List<OrderSummaryResponse> summaries = page.getContent().stream()
                .map(this::mapToSummary)
                .toList();

        return new PageResponse<>(
                summaries,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    @Transactional
    public OrderResponse claimOrder(UUID managerId, String orderNumber) {
        OrderEntity order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order " + orderNumber + " not found"));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new InvalidOperationException("Cannot claim a cancelled order");
        }

        // Atomic claim check
        if (order.getClaimedBy() != null && !order.getClaimedBy().equals(managerId)) {
            throw new ResourceConflictException("Order " + orderNumber + " has already been claimed by another manager (" + order.getClaimedBy() + ")");
        }

        UUID previousClaimant = order.getClaimedBy();
        order.setClaimedBy(managerId);
        order.setClaimedAt(Instant.now());

        if (order.getStatus() == OrderStatus.CONFIRMED) {
            order.setStatus(OrderStatus.PROCESSING);
        }

        OrderEntity saved = orderRepository.save(order);

        // Audit claim history
        claimHistoryRepository.save(new OrderClaimHistoryEntity(
                saved.getId(),
                managerId,
                OrderClaimAction.CLAIMED,
                previousClaimant,
                managerId,
                "Order claimed by manager from shared queue"
        ));

        return checkoutOrchestrationService.mapToOrderResponse(saved);
    }

    @Transactional
    public OrderResponse reassignOrder(UUID ownerId, String orderNumber, ReassignOrderRequest request) {
        OrderEntity order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order " + orderNumber + " not found"));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new InvalidOperationException("Cannot reassign a cancelled order");
        }

        UUID previousManager = order.getClaimedBy();
        order.setClaimedBy(request.targetManagerId());
        order.setClaimedAt(Instant.now());

        OrderEntity saved = orderRepository.save(order);

        // Audit reassignment
        claimHistoryRepository.save(new OrderClaimHistoryEntity(
                saved.getId(),
                ownerId,
                OrderClaimAction.REASSIGNED,
                previousManager,
                request.targetManagerId(),
                request.reason() != null ? request.reason() : "Order reassigned by Owner"
        ));

        return checkoutOrchestrationService.mapToOrderResponse(saved);
    }

    @Transactional
    public OrderResponse dispatchOrder(UUID actorId, String orderNumber) {
        OrderEntity order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order " + orderNumber + " not found"));

        if (order.getStatus() != OrderStatus.PROCESSING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new InvalidOperationException("Order in status " + order.getStatus() + " cannot be dispatched");
        }

        order.setStatus(OrderStatus.SHIPPED);
        order.setDispatchedAt(Instant.now());

        // Commit inventory stock permanently on dispatch
        for (OrderItemEntity item : order.getItems()) {
            try {
                inventoryClient.commitStock(item.getVariantId());
            } catch (Exception e) {
                // Log and continue
            }
        }

        OrderEntity saved = orderRepository.save(order);

        claimHistoryRepository.save(new OrderClaimHistoryEntity(
                saved.getId(),
                actorId,
                OrderClaimAction.DISPATCHED,
                order.getClaimedBy(),
                order.getClaimedBy(),
                "Order dispatched to transportation agency"
        ));

        saveOutboxEvent("ORDER", saved.getId().toString(), "ORDER_DISPATCHED", new OrderDispatchedPayload(saved.getId(), saved.getOrderNumber(), saved.getDispatchedAt()));

        return checkoutOrchestrationService.mapToOrderResponse(saved);
    }

    @Transactional
    public OrderResponse deliverOrder(UUID actorId, String orderNumber) {
        OrderEntity order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order " + orderNumber + " not found"));

        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new InvalidOperationException("Order must be in SHIPPED status to mark delivered");
        }

        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(Instant.now());

        // If COD, delivery confirms cash collection by transport agency
        if (order.getPaymentMethod() == PaymentMethod.COD) {
            order.setPaymentStatus(PaymentStatus.PAID);
        }

        OrderEntity saved = orderRepository.save(order);

        claimHistoryRepository.save(new OrderClaimHistoryEntity(
                saved.getId(),
                actorId,
                OrderClaimAction.DELIVERED,
                order.getClaimedBy(),
                order.getClaimedBy(),
                "Order delivered successfully"
        ));

        saveOutboxEvent("ORDER", saved.getId().toString(), "ORDER_DELIVERED", new OrderDeliveredPayload(saved.getId(), saved.getOrderNumber(), saved.getDeliveredAt()));

        return checkoutOrchestrationService.mapToOrderResponse(saved);
    }

    @Transactional
    public ShortageIncidentResponse reportShortageIncident(UUID reporterId, String orderNumber, ReportShortageRequest request) {
        OrderEntity order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order " + orderNumber + " not found"));

        // Generate incident number
        String datePrefix = LocalDate.now(ZoneId.of("Asia/Kolkata")).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        String incidentNumber = "SHT-" + datePrefix + "-" + randomSuffix;

        OrderShortageIncidentEntity incident = new OrderShortageIncidentEntity(
                incidentNumber,
                order.getId(),
                request.variantId(),
                reporterId,
                request.expectedQuantity(),
                request.actualQuantity(),
                request.notes()
        );

        OrderShortageIncidentEntity saved = shortageIncidentRepository.save(incident);

        // Audit claim history
        claimHistoryRepository.save(new OrderClaimHistoryEntity(
                order.getId(),
                reporterId,
                OrderClaimAction.SHORTAGE_REPORTED,
                order.getClaimedBy(),
                order.getClaimedBy(),
                "Physical stock shortage reported: " + request.notes() + " (Incident: " + incidentNumber + ")"
        ));

        // Note: Order is deliberately NOT auto-cancelled! Owner is alerted to contact customer.
        saveOutboxEvent("SHORTAGE", saved.getId().toString(), "ORDER_SHORTAGE_REPORTED", saved);

        return mapToIncidentResponse(saved, order.getOrderNumber());
    }

    @Transactional(readOnly = true)
    public List<ShortageIncidentResponse> listShortageIncidents(ShortageIncidentStatus status) {
        List<OrderShortageIncidentEntity> list = (status != null)
                ? shortageIncidentRepository.findByStatusOrderByCreatedAtDesc(status)
                : shortageIncidentRepository.findAllByOrderByCreatedAtDesc();

        return list.stream()
                .map(i -> {
                    String orderNumber = orderRepository.findById(i.getOrderId())
                            .map(OrderEntity::getOrderNumber)
                            .orElse("UNKNOWN");
                    return mapToIncidentResponse(i, orderNumber);
                })
                .toList();
    }

    @Transactional
    public ShortageIncidentResponse resolveShortageIncident(UUID ownerId, String incidentNumber, ResolveShortageRequest request) {
        OrderShortageIncidentEntity incident = shortageIncidentRepository.findByIncidentNumber(incidentNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Shortage incident " + incidentNumber + " not found"));

        incident.setStatus(ShortageIncidentStatus.RESOLVED);
        incident.setResolutionNotes(request.resolutionNotes());
        incident.setResolvedBy(ownerId);
        incident.setResolvedAt(Instant.now());

        OrderShortageIncidentEntity saved = shortageIncidentRepository.save(incident);
        String orderNum = orderRepository.findById(saved.getOrderId()).map(OrderEntity::getOrderNumber).orElse("UNKNOWN");
        return mapToIncidentResponse(saved, orderNum);
    }

    private OrderSummaryResponse mapToSummary(OrderEntity order) {
        int totalItems = order.getItems().stream().mapToInt(OrderItemEntity::getQuantity).sum();
        return new OrderSummaryResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getCustomerId(),
                order.getStatus(),
                order.getPaymentMethod(),
                order.getPaymentStatus(),
                order.getClaimedBy(),
                order.getFinalPayableAmount(),
                totalItems,
                order.getConfirmedAt()
        );
    }

    private ShortageIncidentResponse mapToIncidentResponse(OrderShortageIncidentEntity incident, String orderNumber) {
        return new ShortageIncidentResponse(
                incident.getId(),
                incident.getIncidentNumber(),
                incident.getOrderId(),
                orderNumber,
                incident.getVariantId(),
                incident.getReportedBy(),
                incident.getExpectedQuantity(),
                incident.getActualQuantity(),
                incident.getNotes(),
                incident.getStatus(),
                incident.getResolutionNotes(),
                incident.getResolvedBy(),
                incident.getResolvedAt(),
                incident.getCreatedAt()
        );
    }

    private void saveOutboxEvent(String type, String id, String eventType, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            outboxEventRepository.save(new OutboxEventEntity(type, id, eventType, json));
        } catch (Exception e) {
            // Outbox event serialization error
        }
    }

    public record OrderCancelledPayload(UUID orderId, String orderNumber, String reason, UUID cancelledBy) {}
    public record OrderDispatchedPayload(UUID orderId, String orderNumber, Instant dispatchedAt) {}
    public record OrderDeliveredPayload(UUID orderId, String orderNumber, Instant deliveredAt) {}
}
