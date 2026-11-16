package com.dzfashion.fulfillment.service;

import com.dzfashion.fulfillment.dto.request.CreateShipmentRequest;
import com.dzfashion.fulfillment.dto.response.CustomerTrackingResponse;
import com.dzfashion.fulfillment.dto.response.DeliveryAttemptResponse;
import com.dzfashion.fulfillment.dto.response.PageResponse;
import com.dzfashion.fulfillment.dto.response.ShipmentItemResponse;
import com.dzfashion.fulfillment.dto.response.ShipmentResponse;
import com.dzfashion.fulfillment.entity.OutboxEventEntity;
import com.dzfashion.fulfillment.entity.ShipmentEntity;
import com.dzfashion.fulfillment.entity.ShipmentItemEntity;
import com.dzfashion.fulfillment.enums.ShipmentStatus;
import com.dzfashion.fulfillment.exception.InvalidOperationException;
import com.dzfashion.fulfillment.exception.ResourceConflictException;
import com.dzfashion.fulfillment.exception.ResourceNotFoundException;
import com.dzfashion.fulfillment.repository.OutboxEventRepository;
import com.dzfashion.fulfillment.repository.ShipmentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class ShipmentService {

    private static final Logger log = LoggerFactory.getLogger(ShipmentService.class);

    private final ShipmentRepository shipmentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public ShipmentService(ShipmentRepository shipmentRepository,
                           OutboxEventRepository outboxEventRepository,
                           ObjectMapper objectMapper) {
        this.shipmentRepository = shipmentRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    public ShipmentResponse createShipment(CreateShipmentRequest request) {
        if (shipmentRepository.findByOrderId(request.orderId()).isPresent()) {
            throw new ResourceConflictException("Shipment already exists for order ID: " + request.orderId());
        }

        ShipmentEntity entity = new ShipmentEntity(
                request.orderId(),
                request.orderNumber().trim(),
                request.customerId(),
                request.recipientName().trim(),
                request.recipientPhone().trim(),
                request.shippingAddress().trim(),
                request.city().trim(),
                request.state().trim(),
                request.pincode().trim(),
                request.cod(),
                request.codAmount()
        );

        if (request.items() != null) {
            for (var itemReq : request.items()) {
                ShipmentItemEntity item = new ShipmentItemEntity(
                        itemReq.orderItemId(),
                        itemReq.productId(),
                        itemReq.variantId(),
                        itemReq.productName().trim(),
                        itemReq.sku().trim(),
                        itemReq.size() != null ? itemReq.size().trim() : null,
                        itemReq.color() != null ? itemReq.color().trim() : null,
                        itemReq.binLocation() != null ? itemReq.binLocation().trim() : null,
                        itemReq.quantity()
                );
                entity.addItem(item);
            }
        }

        ShipmentEntity saved = shipmentRepository.save(entity);
        log.info("Created shipment {} for order {}", saved.getId(), saved.getOrderNumber());

        emitOutboxEvent("Shipment", saved.getId().toString(), "SHIPMENT_CREATED", Map.of(
                "shipmentId", saved.getId().toString(),
                "orderId", saved.getOrderId().toString(),
                "orderNumber", saved.getOrderNumber(),
                "status", saved.getStatus().name()
        ));

        return mapToResponse(saved);
    }

    public ShipmentResponse pickAndPackShipment(UUID shipmentId, UUID staffId) {
        ShipmentEntity entity = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with ID: " + shipmentId));

        if (entity.getStatus() != ShipmentStatus.PROCESSING) {
            throw new InvalidOperationException("Cannot pick and pack shipment in status: " + entity.getStatus());
        }

        Instant now = Instant.now();
        entity.setPickedAt(now);
        entity.setPackedAt(now);
        entity.setUpdatedAt(now);

        for (ShipmentItemEntity item : entity.getItems()) {
            item.setPicked(true);
            item.setPacked(true);
        }

        ShipmentEntity saved = shipmentRepository.save(entity);
        log.info("Shipment {} picked and packed by staff {}", saved.getId(), staffId);

        emitOutboxEvent("Shipment", saved.getId().toString(), "SHIPMENT_PACKED", Map.of(
                "shipmentId", saved.getId().toString(),
                "orderId", saved.getOrderId().toString(),
                "staffId", staffId != null ? staffId.toString() : "system",
                "packedAt", now.toString()
        ));

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public ShipmentResponse getShipmentById(UUID shipmentId) {
        ShipmentEntity entity = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with ID: " + shipmentId));
        return mapToResponse(entity);
    }

    @Transactional(readOnly = true)
    public ShipmentResponse getShipmentByOrderId(UUID orderId) {
        ShipmentEntity entity = shipmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found for order ID: " + orderId));
        return mapToResponse(entity);
    }

    @Transactional(readOnly = true)
    public CustomerTrackingResponse getCustomerTracking(UUID orderId) {
        ShipmentEntity entity = shipmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Tracking information not found for order ID: " + orderId));

        String agencyName = entity.getTransportAgency() != null ? entity.getTransportAgency().getName() : null;

        return new CustomerTrackingResponse(
                entity.getOrderId(),
                entity.getOrderNumber(),
                entity.getStatus().toCustomerStatus(),
                agencyName,
                entity.getTrackingReference(),
                entity.getDispatchDate(),
                entity.getDeliveryDate(),
                entity.getDeliveryAttempts(),
                entity.getEligibleReturnUntil()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<ShipmentResponse> getCustomerShipments(UUID customerId, Pageable pageable) {
        Page<ShipmentEntity> page = shipmentRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable);
        List<ShipmentResponse> content = page.getContent().stream().map(this::mapToResponse).toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    @Transactional(readOnly = true)
    public PageResponse<ShipmentResponse> getAllShipments(Pageable pageable) {
        Page<ShipmentEntity> page = shipmentRepository.findAllByOrderByCreatedAtDesc(pageable);
        List<ShipmentResponse> content = page.getContent().stream().map(this::mapToResponse).toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    @Transactional(readOnly = true)
    public List<ShipmentResponse> getShipmentsByStatus(ShipmentStatus status) {
        return shipmentRepository.findByStatus(status).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public ShipmentResponse mapToResponse(ShipmentEntity entity) {
        UUID agencyId = entity.getTransportAgency() != null ? entity.getTransportAgency().getId() : null;
        String agencyName = entity.getTransportAgency() != null ? entity.getTransportAgency().getName() : null;
        UUID batchId = entity.getDailyDispatchBatch() != null ? entity.getDailyDispatchBatch().getId() : null;

        List<ShipmentItemResponse> itemResponses = entity.getItems().stream()
                .map(item -> new ShipmentItemResponse(
                        item.getId(),
                        item.getOrderItemId(),
                        item.getProductId(),
                        item.getVariantId(),
                        item.getProductName(),
                        item.getSku(),
                        item.getSize(),
                        item.getColor(),
                        item.getBinLocation(),
                        item.getQuantity(),
                        item.getPickedQuantity(),
                        item.isPicked(),
                        item.isPacked()
                )).toList();

        List<DeliveryAttemptResponse> attemptResponses = entity.getAttemptLogs().stream()
                .map(att -> new DeliveryAttemptResponse(
                        att.getId(),
                        att.getAttemptNumber(),
                        att.getAttemptedAt(),
                        att.getResult(),
                        att.getNotes(),
                        att.getRecordedBy()
                )).toList();

        return new ShipmentResponse(
                entity.getId(),
                entity.getOrderId(),
                entity.getOrderNumber(),
                entity.getCustomerId(),
                entity.getRecipientName(),
                entity.getRecipientPhone(),
                entity.getShippingAddress(),
                entity.getCity(),
                entity.getState(),
                entity.getPincode(),
                agencyId,
                agencyName,
                entity.getTrackingReference(),
                entity.getStatus(),
                entity.getDispatchDate(),
                entity.getDeliveryDate(),
                entity.getDeliveryAttempts(),
                entity.isCod(),
                entity.getCodAmount(),
                entity.getCodCollectedAmount(),
                batchId,
                entity.getPickedAt(),
                entity.getPackedAt(),
                entity.getDispatchedAt(),
                entity.getReturnedToWarehouseAt(),
                entity.getEligibleReturnUntil(),
                entity.getCodSettlementBatchId(),
                entity.getNotes(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                itemResponses,
                attemptResponses
        );
    }

    private void emitOutboxEvent(String aggregateType, String aggregateId, String eventType, Object payload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            outboxEventRepository.save(new OutboxEventEntity(aggregateType, aggregateId, eventType, payloadJson));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox event payload for {} {}", aggregateType, aggregateId, e);
            throw new RuntimeException("Error serializing outbox event", e);
        }
    }
}
