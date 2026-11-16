package com.dzfashion.fulfillment.service;

import com.dzfashion.fulfillment.config.FulfillmentProperties;
import com.dzfashion.fulfillment.dto.request.ReceiveReturnAtWarehouseRequest;
import com.dzfashion.fulfillment.dto.request.RecordDeliveryAttemptRequest;
import com.dzfashion.fulfillment.dto.request.RecordDeliverySuccessRequest;
import com.dzfashion.fulfillment.dto.response.ShipmentResponse;
import com.dzfashion.fulfillment.entity.DeliveryAttemptLogEntity;
import com.dzfashion.fulfillment.entity.OutboxEventEntity;
import com.dzfashion.fulfillment.entity.ShipmentEntity;
import com.dzfashion.fulfillment.enums.ShipmentStatus;
import com.dzfashion.fulfillment.exception.InvalidOperationException;
import com.dzfashion.fulfillment.exception.ResourceNotFoundException;
import com.dzfashion.fulfillment.repository.DeliveryAttemptLogRepository;
import com.dzfashion.fulfillment.repository.OutboxEventRepository;
import com.dzfashion.fulfillment.repository.ShipmentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class ShipmentDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(ShipmentDeliveryService.class);

    private final ShipmentRepository shipmentRepository;
    private final DeliveryAttemptLogRepository deliveryAttemptLogRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final FulfillmentProperties fulfillmentProperties;
    private final ShipmentService shipmentService;
    private final ObjectMapper objectMapper;

    public ShipmentDeliveryService(ShipmentRepository shipmentRepository,
                                   DeliveryAttemptLogRepository deliveryAttemptLogRepository,
                                   OutboxEventRepository outboxEventRepository,
                                   FulfillmentProperties fulfillmentProperties,
                                   ShipmentService shipmentService,
                                   ObjectMapper objectMapper) {
        this.shipmentRepository = shipmentRepository;
        this.deliveryAttemptLogRepository = deliveryAttemptLogRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.fulfillmentProperties = fulfillmentProperties;
        this.shipmentService = shipmentService;
        this.objectMapper = objectMapper;
    }

    public ShipmentResponse recordDeliveryAttempt(UUID shipmentId, RecordDeliveryAttemptRequest request, UUID staffId) {
        ShipmentEntity shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with ID: " + shipmentId));

        if (shipment.getStatus() == ShipmentStatus.DELIVERED) {
            throw new InvalidOperationException("Shipment " + shipment.getOrderNumber() + " is already delivered.");
        }
        if (shipment.getStatus() == ShipmentStatus.RETURNING_TO_WAREHOUSE || shipment.getStatus() == ShipmentStatus.RETURNED_TO_WAREHOUSE) {
            throw new InvalidOperationException("Shipment " + shipment.getOrderNumber() + " is returning or returned to warehouse.");
        }
        if (shipment.getStatus() == ShipmentStatus.PROCESSING) {
            throw new InvalidOperationException("Shipment " + shipment.getOrderNumber() + " is still in processing and has not been dispatched.");
        }

        int currentAttempts = shipment.getDeliveryAttempts();
        int maxAttempts = fulfillmentProperties.getMaxDeliveryAttempts();

        if (currentAttempts >= maxAttempts) {
            throw new InvalidOperationException("Maximum " + maxAttempts + " delivery attempts reached for shipment " + shipment.getOrderNumber() + ". Parcel must be returned to warehouse.");
        }

        int newAttemptNumber = currentAttempts + 1;
        Instant attemptTime = request.attemptedAt() != null ? request.attemptedAt() : Instant.now();

        DeliveryAttemptLogEntity attemptLog = new DeliveryAttemptLogEntity(
                newAttemptNumber,
                attemptTime,
                request.result(),
                request.notes() != null ? request.notes().trim() : null,
                staffId
        );
        shipment.addAttemptLog(attemptLog);
        shipment.setDeliveryAttempts(newAttemptNumber);
        shipment.setUpdatedAt(Instant.now());

        if (newAttemptNumber == 1) {
            shipment.setStatus(ShipmentStatus.DELIVERY_ATTEMPTED_1);
            log.info("Recorded 1st delivery attempt for shipment {} ({})", shipment.getId(), shipment.getOrderNumber());
            emitOutboxEvent("Shipment", shipment.getId().toString(), "DELIVERY_ATTEMPTED_1", Map.of(
                    "shipmentId", shipment.getId().toString(),
                    "orderId", shipment.getOrderId().toString(),
                    "attemptNumber", 1,
                    "result", request.result().name()
            ));
        } else if (newAttemptNumber == 2) {
            // Second attempt failed -> triggers return to Surat warehouse
            shipment.setStatus(ShipmentStatus.RETURNING_TO_WAREHOUSE);
            log.info("Recorded 2nd delivery attempt for shipment {} ({}). Initiating return to Surat warehouse.",
                    shipment.getId(), shipment.getOrderNumber());
            emitOutboxEvent("Shipment", shipment.getId().toString(), "SHIPMENT_RETURN_INITIATED", Map.of(
                    "shipmentId", shipment.getId().toString(),
                    "orderId", shipment.getOrderId().toString(),
                    "attemptNumber", 2,
                    "result", request.result().name(),
                    "reason", "EXCEEDED_MAX_ATTEMPTS"
            ));
        }

        ShipmentEntity saved = shipmentRepository.save(shipment);
        return shipmentService.mapToResponse(saved);
    }

    public ShipmentResponse recordDeliverySuccess(UUID shipmentId, RecordDeliverySuccessRequest request, UUID staffId) {
        ShipmentEntity shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with ID: " + shipmentId));

        if (shipment.getStatus() == ShipmentStatus.DELIVERED) {
            throw new InvalidOperationException("Shipment " + shipment.getOrderNumber() + " is already marked as DELIVERED.");
        }
        if (shipment.getStatus() == ShipmentStatus.RETURNING_TO_WAREHOUSE || shipment.getStatus() == ShipmentStatus.RETURNED_TO_WAREHOUSE) {
            throw new InvalidOperationException("Shipment " + shipment.getOrderNumber() + " is already returning or returned to warehouse.");
        }
        if (shipment.getStatus() == ShipmentStatus.PROCESSING) {
            throw new InvalidOperationException("Shipment " + shipment.getOrderNumber() + " has not been dispatched yet.");
        }

        Instant now = Instant.now();
        shipment.setStatus(ShipmentStatus.DELIVERED);
        shipment.setDeliveryDate(request.deliveryDate());
        shipment.setEligibleReturnUntil(now.plus(fulfillmentProperties.getReturnWindowDays(), ChronoUnit.DAYS));

        if (shipment.isCod()) {
            if (request.codCollectedAmount() != null) {
                shipment.setCodCollectedAmount(request.codCollectedAmount());
            } else {
                shipment.setCodCollectedAmount(shipment.getCodAmount());
            }
        }

        if (request.notes() != null && !request.notes().isBlank()) {
            String combined = (shipment.getNotes() != null ? shipment.getNotes() + "\n" : "") + "Delivery note: " + request.notes().trim();
            shipment.setNotes(combined);
        }

        shipment.setUpdatedAt(now);
        ShipmentEntity saved = shipmentRepository.save(shipment);

        log.info("Shipment {} ({}) marked DELIVERED on {}. Return window valid until {}",
                saved.getId(), saved.getOrderNumber(), saved.getDeliveryDate(), saved.getEligibleReturnUntil());

        emitOutboxEvent("Shipment", saved.getId().toString(), "SHIPMENT_DELIVERED", Map.of(
                "shipmentId", saved.getId().toString(),
                "orderId", saved.getOrderId().toString(),
                "orderNumber", saved.getOrderNumber(),
                "deliveryDate", saved.getDeliveryDate().toString(),
                "isCod", saved.isCod(),
                "codCollectedAmount", saved.getCodCollectedAmount() != null ? saved.getCodCollectedAmount().toString() : "0.00",
                "eligibleReturnUntil", saved.getEligibleReturnUntil().toString()
        ));

        return shipmentService.mapToResponse(saved);
    }

    public ShipmentResponse receiveReturnAtWarehouse(UUID shipmentId, ReceiveReturnAtWarehouseRequest request, UUID staffId) {
        ShipmentEntity shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with ID: " + shipmentId));

        if (shipment.getStatus() != ShipmentStatus.RETURNING_TO_WAREHOUSE) {
            throw new InvalidOperationException("Cannot receive return at warehouse for shipment in status: " + shipment.getStatus() + ". Expected RETURNING_TO_WAREHOUSE.");
        }

        Instant now = Instant.now();
        shipment.setStatus(ShipmentStatus.RETURNED_TO_WAREHOUSE);
        shipment.setReturnedToWarehouseAt(now);

        String noteEntry = "Warehouse Return Intake (" + fulfillmentProperties.getWarehouseName() + "): " + request.conditionNotes().trim();
        shipment.setNotes(shipment.getNotes() != null ? shipment.getNotes() + "\n" + noteEntry : noteEntry);
        shipment.setUpdatedAt(now);

        ShipmentEntity saved = shipmentRepository.save(shipment);
        log.info("Shipment {} ({}) physically received at Surat warehouse. Condition: {}",
                saved.getId(), saved.getOrderNumber(), request.conditionNotes());

        emitOutboxEvent("Shipment", saved.getId().toString(), "SHIPMENT_RETURNED_TO_WAREHOUSE", Map.of(
                "shipmentId", saved.getId().toString(),
                "orderId", saved.getOrderId().toString(),
                "orderNumber", saved.getOrderNumber(),
                "warehouseName", fulfillmentProperties.getWarehouseName(),
                "conditionNotes", request.conditionNotes().trim(),
                "returnedAt", now.toString()
        ));

        return shipmentService.mapToResponse(saved);
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
