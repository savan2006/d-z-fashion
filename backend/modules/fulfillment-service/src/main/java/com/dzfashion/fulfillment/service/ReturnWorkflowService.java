package com.dzfashion.fulfillment.service;

import com.dzfashion.fulfillment.config.FulfillmentProperties;
import com.dzfashion.fulfillment.dto.request.ConfirmExchangePaymentRequest;
import com.dzfashion.fulfillment.dto.request.CreateReturnRequest;
import com.dzfashion.fulfillment.dto.request.PerformInspectionRequest;
import com.dzfashion.fulfillment.dto.request.RecordPickupRequest;
import com.dzfashion.fulfillment.dto.request.RecordReturnArrivalRequest;
import com.dzfashion.fulfillment.dto.request.RejectRefundRequest;
import com.dzfashion.fulfillment.dto.request.SchedulePickupRequest;
import com.dzfashion.fulfillment.dto.response.PageResponse;
import com.dzfashion.fulfillment.dto.response.ReturnEligibilityResponse;
import com.dzfashion.fulfillment.dto.response.ReturnInspectionResponse;
import com.dzfashion.fulfillment.dto.response.ReturnRequestResponse;
import com.dzfashion.fulfillment.entity.OutboxEventEntity;
import com.dzfashion.fulfillment.entity.ReturnInspectionEntity;
import com.dzfashion.fulfillment.entity.ReturnRequestEntity;
import com.dzfashion.fulfillment.entity.ShipmentEntity;
import com.dzfashion.fulfillment.entity.ShipmentItemEntity;
import com.dzfashion.fulfillment.entity.TransportAgencyEntity;
import com.dzfashion.fulfillment.enums.ReturnInspectionResult;
import com.dzfashion.fulfillment.enums.ReturnRequestStatus;
import com.dzfashion.fulfillment.enums.ReturnResolutionType;
import com.dzfashion.fulfillment.exception.InvalidOperationException;
import com.dzfashion.fulfillment.exception.ResourceNotFoundException;
import com.dzfashion.fulfillment.repository.OutboxEventRepository;
import com.dzfashion.fulfillment.repository.ReturnInspectionRepository;
import com.dzfashion.fulfillment.repository.ReturnRequestRepository;
import com.dzfashion.fulfillment.repository.ShipmentRepository;
import com.dzfashion.fulfillment.repository.TransportAgencyRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class ReturnWorkflowService {

    private static final Logger log = LoggerFactory.getLogger(ReturnWorkflowService.class);

    private final ReturnRequestRepository returnRequestRepository;
    private final ReturnInspectionRepository returnInspectionRepository;
    private final ShipmentRepository shipmentRepository;
    private final TransportAgencyRepository transportAgencyRepository;
    private final ReturnEligibilityService returnEligibilityService;
    private final OutboxEventRepository outboxEventRepository;
    private final FulfillmentProperties fulfillmentProperties;
    private final ObjectMapper objectMapper;

    public ReturnWorkflowService(ReturnRequestRepository returnRequestRepository,
                                 ReturnInspectionRepository returnInspectionRepository,
                                 ShipmentRepository shipmentRepository,
                                 TransportAgencyRepository transportAgencyRepository,
                                 ReturnEligibilityService returnEligibilityService,
                                 OutboxEventRepository outboxEventRepository,
                                 FulfillmentProperties fulfillmentProperties,
                                 ObjectMapper objectMapper) {
        this.returnRequestRepository = returnRequestRepository;
        this.returnInspectionRepository = returnInspectionRepository;
        this.shipmentRepository = shipmentRepository;
        this.transportAgencyRepository = transportAgencyRepository;
        this.returnEligibilityService = returnEligibilityService;
        this.outboxEventRepository = outboxEventRepository;
        this.fulfillmentProperties = fulfillmentProperties;
        this.objectMapper = objectMapper;
    }

    public ReturnRequestResponse createReturnRequest(CreateReturnRequest request, UUID customerId) {
        ReturnEligibilityResponse eligibility = returnEligibilityService.checkEligibility(request.orderId());
        if (!eligibility.eligible()) {
            throw new InvalidOperationException("Order is not eligible for return/exchange: " + eligibility.reasonIfNotEligible());
        }

        ShipmentEntity shipment = shipmentRepository.findByOrderId(request.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found for order: " + request.orderId()));

        ShipmentItemEntity itemEntity = shipment.getItems().stream()
                .filter(i -> i.getOrderItemId().equals(request.orderItemId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item " + request.orderItemId() + " not found in order shipment"));

        int activeReturnQty = returnRequestRepository.countActiveReturnQuantityByOrderItemId(request.orderItemId());
        int eligibleQty = itemEntity.getQuantity() - activeReturnQty;
        if (request.quantity() > eligibleQty) {
            throw new InvalidOperationException("Requested return quantity (" + request.quantity() +
                    ") exceeds eligible quantity (" + eligibleQty + "). Already returned/active: " + activeReturnQty);
        }

        if (request.photoUrls() == null || request.photoUrls().isEmpty()) {
            throw new InvalidOperationException("At least one product photo is mandatory for quality inspection proof.");
        }

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String returnNumber = "RET-" + dateStr + "-" + suffix;

        ReturnResolutionType resolutionType = request.resolutionType() != null ? request.resolutionType() : ReturnResolutionType.EXCHANGE;

        ReturnRequestEntity entity = new ReturnRequestEntity(
                returnNumber,
                request.orderId(),
                request.orderNumber().trim(),
                request.orderItemId(),
                customerId,
                request.sku().trim(),
                request.productName().trim(),
                request.quantity(),
                request.originalUnitPrice(),
                request.reason(),
                resolutionType,
                request.photoUrls(),
                request.customerNotes(),
                shipment.isCod()
        );

        if (resolutionType == ReturnResolutionType.EXCHANGE) {
            entity.setReplacementProductId(request.replacementProductId());
            entity.setReplacementVariantId(request.replacementVariantId());
            entity.setReplacementSku(request.replacementSku());
            entity.setReplacementProductName(request.replacementProductName());
            entity.setReplacementUnitPrice(request.replacementUnitPrice());

            if (request.replacementUnitPrice() != null) {
                BigDecimal diffPerUnit = request.replacementUnitPrice().subtract(request.originalUnitPrice());
                BigDecimal totalDiff = diffPerUnit.multiply(BigDecimal.valueOf(request.quantity()));
                entity.setPriceDifference(totalDiff);
                if (totalDiff.compareTo(BigDecimal.ZERO) > 0) {
                    entity.setPriceDifferenceSettled(false);
                } else {
                    entity.setPriceDifferenceSettled(true);
                }
            }
            entity.setStatus(ReturnRequestStatus.REQUESTED);
        } else {
            entity.setStatus(ReturnRequestStatus.REFUND_PENDING_OWNER_APPROVAL);
            BigDecimal refundAmt = request.originalUnitPrice().multiply(BigDecimal.valueOf(request.quantity()));
            entity.setRefundAmount(refundAmt);
        }

        ReturnRequestEntity saved = returnRequestRepository.save(entity);
        log.info("Created return request {} for order {} (item: {}, qty: {}, resolution: {})",
                saved.getReturnNumber(), saved.getOrderNumber(), saved.getSku(), saved.getQuantity(), saved.getResolutionType());

        emitOutboxEvent("ReturnRequest", saved.getId().toString(), "RETURN_REQUEST_CREATED", Map.of(
                "returnId", saved.getId().toString(),
                "returnNumber", saved.getReturnNumber(),
                "orderId", saved.getOrderId().toString(),
                "resolutionType", saved.getResolutionType().name(),
                "reason", saved.getReason().name()
        ));

        return mapToResponse(saved);
    }

    public ReturnRequestResponse schedulePickup(UUID returnId, SchedulePickupRequest request, UUID staffId) {
        ReturnRequestEntity entity = returnRequestRepository.findById(returnId)
                .orElseThrow(() -> new ResourceNotFoundException("Return request not found with ID: " + returnId));

        if (entity.getStatus() != ReturnRequestStatus.REQUESTED && entity.getStatus() != ReturnRequestStatus.REFUND_PENDING_OWNER_APPROVAL) {
            throw new InvalidOperationException("Cannot schedule pickup for return request in status: " + entity.getStatus());
        }

        TransportAgencyEntity agency = transportAgencyRepository.findById(request.transportAgencyId())
                .orElseThrow(() -> new ResourceNotFoundException("Transport agency not found with ID: " + request.transportAgencyId()));

        entity.setTransportAgency(agency);
        entity.setPickupScheduledDate(request.pickupScheduledDate());
        entity.setPickupReference(request.pickupReference() != null ? request.pickupReference().trim() : null);
        entity.setStatus(ReturnRequestStatus.PICKUP_SCHEDULED);
        entity.setUpdatedAt(Instant.now());

        ReturnRequestEntity saved = returnRequestRepository.save(entity);
        log.info("Return {} pickup scheduled on {} with agency {}", saved.getReturnNumber(), saved.getPickupScheduledDate(), agency.getName());

        emitOutboxEvent("ReturnRequest", saved.getId().toString(), "RETURN_PICKUP_SCHEDULED", Map.of(
                "returnId", saved.getId().toString(),
                "returnNumber", saved.getReturnNumber(),
                "agencyName", agency.getName(),
                "pickupDate", saved.getPickupScheduledDate().toString()
        ));

        return mapToResponse(saved);
    }

    public ReturnRequestResponse recordPickup(UUID returnId, RecordPickupRequest request, UUID staffId) {
        ReturnRequestEntity entity = returnRequestRepository.findById(returnId)
                .orElseThrow(() -> new ResourceNotFoundException("Return request not found with ID: " + returnId));

        if (entity.getStatus() != ReturnRequestStatus.PICKUP_SCHEDULED) {
            throw new InvalidOperationException("Cannot record pickup for return request in status: " + entity.getStatus());
        }

        Instant pickedUpTime = request.pickedUpAt() != null ? request.pickedUpAt() : Instant.now();
        entity.setPickedUpAt(pickedUpTime);
        entity.setStatus(ReturnRequestStatus.IN_TRANSIT_TO_SURAT);
        entity.setUpdatedAt(Instant.now());

        ReturnRequestEntity saved = returnRequestRepository.save(entity);
        log.info("Return {} picked up from customer, in transit to Surat warehouse", saved.getReturnNumber());

        emitOutboxEvent("ReturnRequest", saved.getId().toString(), "RETURN_IN_TRANSIT", Map.of(
                "returnId", saved.getId().toString(),
                "returnNumber", saved.getReturnNumber(),
                "pickedUpAt", pickedUpTime.toString()
        ));

        return mapToResponse(saved);
    }

    public ReturnRequestResponse recordArrivalAtSurat(UUID returnId, RecordReturnArrivalRequest request, UUID staffId) {
        ReturnRequestEntity entity = returnRequestRepository.findById(returnId)
                .orElseThrow(() -> new ResourceNotFoundException("Return request not found with ID: " + returnId));

        if (entity.getStatus() != ReturnRequestStatus.IN_TRANSIT_TO_SURAT && entity.getStatus() != ReturnRequestStatus.PICKUP_SCHEDULED) {
            throw new InvalidOperationException("Cannot record Surat warehouse arrival for return request in status: " + entity.getStatus());
        }

        Instant receivedTime = request.receivedAt() != null ? request.receivedAt() : Instant.now();
        entity.setReceivedAtWarehouseAt(receivedTime);
        entity.setStatus(ReturnRequestStatus.RECEIVED_AT_SURAT);
        entity.setUpdatedAt(Instant.now());

        ReturnRequestEntity saved = returnRequestRepository.save(entity);
        log.info("Return {} physically received at Surat warehouse", saved.getReturnNumber());

        emitOutboxEvent("ReturnRequest", saved.getId().toString(), "RETURN_RECEIVED_AT_SURAT", Map.of(
                "returnId", saved.getId().toString(),
                "returnNumber", saved.getReturnNumber(),
                "warehouse", fulfillmentProperties.getWarehouseName(),
                "receivedAt", receivedTime.toString()
        ));

        return mapToResponse(saved);
    }

    public ReturnRequestResponse performInspection(UUID returnId, PerformInspectionRequest request, UUID staffId) {
        ReturnRequestEntity entity = returnRequestRepository.findById(returnId)
                .orElseThrow(() -> new ResourceNotFoundException("Return request not found with ID: " + returnId));

        if (entity.getStatus() != ReturnRequestStatus.RECEIVED_AT_SURAT) {
            throw new InvalidOperationException("Cannot inspect parcel. Physical parcel must first reach Surat warehouse. Current status: " + entity.getStatus());
        }

        ReturnInspectionEntity inspection = new ReturnInspectionEntity(
                entity,
                entity.getOrderId(),
                staffId != null ? staffId : UUID.randomUUID(),
                request.result(),
                request.disposition(),
                request.conditionNotes().trim(),
                request.photoUrls()
        );

        entity.setInspection(inspection);
        entity.setUpdatedAt(Instant.now());

        if (request.result() == ReturnInspectionResult.PASSED) {
            if (entity.getResolutionType() == ReturnResolutionType.EXCHANGE) {
                entity.setStatus(ReturnRequestStatus.EXCHANGE_PREPARED);
                // If exchange is cheaper, difference becomes store credit
                if (entity.getPriceDifference().compareTo(BigDecimal.ZERO) < 0) {
                    BigDecimal storeCreditAmount = entity.getPriceDifference().abs();
                    emitOutboxEvent("StoreCredit", entity.getCustomerId().toString(), "RETURN_STORE_CREDIT_ISSUED", Map.of(
                            "customerId", entity.getCustomerId().toString(),
                            "returnId", entity.getId().toString(),
                            "returnNumber", entity.getReturnNumber(),
                            "amount", storeCreditAmount.toString(),
                            "reason", "EXCHANGE_PRICE_DIFFERENCE_CREDIT"
                    ));
                }
            } else {
                entity.setStatus(ReturnRequestStatus.REFUND_PENDING_OWNER_APPROVAL);
            }
        } else {
            entity.setStatus(ReturnRequestStatus.REJECTED);
        }

        ReturnRequestEntity saved = returnRequestRepository.save(entity);
        log.info("Return {} inspected by staff {}. Result: {}, Disposition: {}",
                saved.getReturnNumber(), staffId, request.result(), request.disposition());

        emitOutboxEvent("ReturnRequest", saved.getId().toString(), "RETURN_INSPECTED", Map.of(
                "returnId", saved.getId().toString(),
                "returnNumber", saved.getReturnNumber(),
                "result", request.result().name(),
                "disposition", request.disposition().name()
        ));

        return mapToResponse(saved);
    }

    public ReturnRequestResponse confirmExchangePayment(UUID returnId, ConfirmExchangePaymentRequest request, UUID staffId) {
        ReturnRequestEntity entity = returnRequestRepository.findById(returnId)
                .orElseThrow(() -> new ResourceNotFoundException("Return request not found with ID: " + returnId));

        if (entity.isPriceDifferenceSettled()) {
            return mapToResponse(entity);
        }

        entity.setPriceDifferenceSettled(true);
        entity.setUpdatedAt(Instant.now());

        ReturnRequestEntity saved = returnRequestRepository.save(entity);
        log.info("Exchange payment difference settled for return {}", saved.getReturnNumber());

        emitOutboxEvent("ReturnRequest", saved.getId().toString(), "EXCHANGE_PAYMENT_SETTLED", Map.of(
                "returnId", saved.getId().toString(),
                "returnNumber", saved.getReturnNumber(),
                "priceDifference", saved.getPriceDifference().toString()
        ));

        return mapToResponse(saved);
    }

    public ReturnRequestResponse dispatchExchangeReplacement(UUID returnId, UUID staffId) {
        ReturnRequestEntity entity = returnRequestRepository.findById(returnId)
                .orElseThrow(() -> new ResourceNotFoundException("Return request not found with ID: " + returnId));

        if (entity.getStatus() != ReturnRequestStatus.EXCHANGE_PREPARED) {
            throw new InvalidOperationException("Cannot dispatch replacement. Return request must be in EXCHANGE_PREPARED status. Current: " + entity.getStatus());
        }

        if (entity.getInspection() == null || entity.getInspection().getResult() != ReturnInspectionResult.PASSED) {
            throw new InvalidOperationException("Cannot dispatch replacement: parcel must reach Surat warehouse and pass inspection first.");
        }

        if (!entity.isPriceDifferenceSettled()) {
            throw new InvalidOperationException("Cannot dispatch replacement: exchange price difference of ₹" +
                    entity.getPriceDifference() + " has not been settled yet.");
        }

        entity.setStatus(ReturnRequestStatus.EXCHANGE_DISPATCHED);
        entity.setUpdatedAt(Instant.now());

        ReturnRequestEntity saved = returnRequestRepository.save(entity);
        log.info("Exchange replacement dispatched for return {}", saved.getReturnNumber());

        emitOutboxEvent("ReturnRequest", saved.getId().toString(), "EXCHANGE_REPLACEMENT_DISPATCHED", Map.of(
                "returnId", saved.getId().toString(),
                "returnNumber", saved.getReturnNumber(),
                "replacementSku", saved.getReplacementSku(),
                "orderNumber", saved.getOrderNumber()
        ));

        return mapToResponse(saved);
    }

    public ReturnRequestResponse requestRefund(UUID returnId, String recommendationNotes, UUID managerId) {
        ReturnRequestEntity entity = returnRequestRepository.findById(returnId)
                .orElseThrow(() -> new ResourceNotFoundException("Return request not found with ID: " + returnId));

        entity.setResolutionType(ReturnResolutionType.REFUND);
        entity.setStatus(ReturnRequestStatus.REFUND_PENDING_OWNER_APPROVAL);
        if (recommendationNotes != null && !recommendationNotes.isBlank()) {
            entity.setCustomerNotes((entity.getCustomerNotes() != null ? entity.getCustomerNotes() + "\n" : "") + "Manager recommendation: " + recommendationNotes.trim());
        }
        entity.setUpdatedAt(Instant.now());

        ReturnRequestEntity saved = returnRequestRepository.save(entity);
        log.info("Manager {} recommended exceptional refund for return {}", managerId, saved.getReturnNumber());

        emitOutboxEvent("ReturnRequest", saved.getId().toString(), "REFUND_RECOMMENDED_BY_MANAGER", Map.of(
                "returnId", saved.getId().toString(),
                "returnNumber", saved.getReturnNumber()
        ));

        return mapToResponse(saved);
    }

    public ReturnRequestResponse approveRefund(UUID returnId, UUID ownerId) {
        ReturnRequestEntity entity = returnRequestRepository.findById(returnId)
                .orElseThrow(() -> new ResourceNotFoundException("Return request not found with ID: " + returnId));

        if (entity.getStatus() != ReturnRequestStatus.REFUND_PENDING_OWNER_APPROVAL) {
            throw new InvalidOperationException("Return request " + entity.getReturnNumber() + " is not pending Owner refund approval. Current status: " + entity.getStatus());
        }

        Instant now = Instant.now();
        Instant deadline = now.plus(7, ChronoUnit.DAYS); // Exactly 7 days deadline after Owner approval

        entity.setStatus(ReturnRequestStatus.REFUND_APPROVED);
        entity.setRefundApprovedBy(ownerId);
        entity.setRefundApprovedAt(now);
        entity.setRefundDeadline(deadline);
        entity.setUpdatedAt(now);

        ReturnRequestEntity saved = returnRequestRepository.save(entity);
        log.info("Refund for return {} approved by Owner {}. 7-day processing deadline: {}",
                saved.getReturnNumber(), ownerId, deadline);

        if (entity.isCodRefund()) {
            emitOutboxEvent("StoreCredit", entity.getCustomerId().toString(), "COD_REFUND_APPROVED_AS_STORE_CREDIT", Map.of(
                    "customerId", entity.getCustomerId().toString(),
                    "returnId", entity.getId().toString(),
                    "returnNumber", entity.getReturnNumber(),
                    "amount", entity.getRefundAmount().toString(),
                    "deadline", deadline.toString()
            ));
        } else {
            emitOutboxEvent("Refund", entity.getOrderId().toString(), "SIMULATED_ONLINE_REFUND_APPROVED", Map.of(
                    "customerId", entity.getCustomerId().toString(),
                    "returnId", entity.getId().toString(),
                    "returnNumber", entity.getReturnNumber(),
                    "amount", entity.getRefundAmount().toString(),
                    "deadline", deadline.toString(),
                    "type", "SIMULATED_VIRTUAL_REFUND"
            ));
        }

        return mapToResponse(saved);
    }

    public ReturnRequestResponse rejectRefund(UUID returnId, RejectRefundRequest request, UUID ownerId) {
        ReturnRequestEntity entity = returnRequestRepository.findById(returnId)
                .orElseThrow(() -> new ResourceNotFoundException("Return request not found with ID: " + returnId));

        entity.setStatus(ReturnRequestStatus.REJECTED);
        entity.setRefundRejectionReason(request.reason().trim());
        entity.setUpdatedAt(Instant.now());

        ReturnRequestEntity saved = returnRequestRepository.save(entity);
        log.info("Refund for return {} rejected by Owner {}. Reason: {}", saved.getReturnNumber(), ownerId, request.reason());

        emitOutboxEvent("ReturnRequest", saved.getId().toString(), "REFUND_REJECTED_BY_OWNER", Map.of(
                "returnId", saved.getId().toString(),
                "returnNumber", saved.getReturnNumber() != null ? saved.getReturnNumber() : "",
                "reason", request.reason().trim()
        ));

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public ReturnRequestResponse getReturnById(UUID id) {
        ReturnRequestEntity entity = returnRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Return request not found with ID: " + id));
        return mapToResponse(entity);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReturnRequestResponse> getReturnsByCustomer(UUID customerId, Pageable pageable) {
        Page<ReturnRequestEntity> page = returnRequestRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable);
        List<ReturnRequestResponse> content = page.getContent().stream().map(this::mapToResponse).toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    @Transactional(readOnly = true)
    public List<ReturnRequestResponse> getReturnsByOrder(UUID orderId) {
        return returnRequestRepository.findByOrderId(orderId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<ReturnRequestResponse> getAllReturns(Pageable pageable) {
        Page<ReturnRequestEntity> page = returnRequestRepository.findAllByOrderByCreatedAtDesc(pageable);
        List<ReturnRequestResponse> content = page.getContent().stream().map(this::mapToResponse).toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    public ReturnRequestResponse mapToResponse(ReturnRequestEntity entity) {
        UUID agencyId = entity.getTransportAgency() != null ? entity.getTransportAgency().getId() : null;
        String agencyName = entity.getTransportAgency() != null ? entity.getTransportAgency().getName() : null;
        UUID replShipmentId = entity.getReplacementShipment() != null ? entity.getReplacementShipment().getId() : null;

        ReturnInspectionResponse inspectionResponse = null;
        if (entity.getInspection() != null) {
            ReturnInspectionEntity insp = entity.getInspection();
            inspectionResponse = new ReturnInspectionResponse(
                    insp.getId(),
                    entity.getId(),
                    insp.getOrderId(),
                    insp.getInspectedBy(),
                    insp.getResult(),
                    insp.getDisposition(),
                    insp.getConditionNotes(),
                    insp.getPhotoUrls(),
                    insp.getInspectedAt()
            );
        }

        return new ReturnRequestResponse(
                entity.getId(),
                entity.getReturnNumber(),
                entity.getOrderId(),
                entity.getOrderNumber(),
                entity.getOrderItemId(),
                entity.getCustomerId(),
                entity.getSku(),
                entity.getProductName(),
                entity.getQuantity(),
                entity.getOriginalUnitPrice(),
                entity.getReason(),
                entity.getResolutionType(),
                entity.getStatus(),
                entity.getPhotoUrls(),
                entity.getCustomerNotes(),
                agencyId,
                agencyName,
                entity.getPickupReference(),
                entity.getPickupScheduledDate(),
                entity.getPickedUpAt(),
                entity.getReceivedAtWarehouseAt(),
                entity.getReplacementProductId(),
                entity.getReplacementVariantId(),
                entity.getReplacementSku(),
                entity.getReplacementProductName(),
                entity.getReplacementUnitPrice(),
                entity.getPriceDifference(),
                entity.isPriceDifferenceSettled(),
                replShipmentId,
                entity.getRefundAmount(),
                entity.getRefundDeadline(),
                entity.getRefundApprovedBy(),
                entity.getRefundApprovedAt(),
                entity.getRefundRejectionReason(),
                entity.isCodRefund(),
                inspectionResponse,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
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
