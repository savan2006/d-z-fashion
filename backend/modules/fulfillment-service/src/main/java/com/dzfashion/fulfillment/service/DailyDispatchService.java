package com.dzfashion.fulfillment.service;

import com.dzfashion.fulfillment.dto.request.CreateDispatchBatchRequest;
import com.dzfashion.fulfillment.dto.request.RecordHandoverRequest;
import com.dzfashion.fulfillment.dto.response.DailyDispatchBatchResponse;
import com.dzfashion.fulfillment.dto.response.PageResponse;
import com.dzfashion.fulfillment.entity.DailyDispatchBatchEntity;
import com.dzfashion.fulfillment.entity.OutboxEventEntity;
import com.dzfashion.fulfillment.entity.ShipmentEntity;
import com.dzfashion.fulfillment.entity.TransportAgencyEntity;
import com.dzfashion.fulfillment.enums.DailyDispatchStatus;
import com.dzfashion.fulfillment.enums.ShipmentStatus;
import com.dzfashion.fulfillment.exception.InvalidOperationException;
import com.dzfashion.fulfillment.exception.ResourceNotFoundException;
import com.dzfashion.fulfillment.repository.DailyDispatchBatchRepository;
import com.dzfashion.fulfillment.repository.OutboxEventRepository;
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class DailyDispatchService {

    private static final Logger log = LoggerFactory.getLogger(DailyDispatchService.class);

    private final DailyDispatchBatchRepository dailyDispatchBatchRepository;
    private final ShipmentRepository shipmentRepository;
    private final TransportAgencyRepository transportAgencyRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public DailyDispatchService(DailyDispatchBatchRepository dailyDispatchBatchRepository,
                                ShipmentRepository shipmentRepository,
                                TransportAgencyRepository transportAgencyRepository,
                                OutboxEventRepository outboxEventRepository,
                                ObjectMapper objectMapper) {
        this.dailyDispatchBatchRepository = dailyDispatchBatchRepository;
        this.shipmentRepository = shipmentRepository;
        this.transportAgencyRepository = transportAgencyRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    public DailyDispatchBatchResponse createDailyDispatchBatch(CreateDispatchBatchRequest request, UUID staffId) {
        TransportAgencyEntity agency = transportAgencyRepository.findById(request.transportAgencyId())
                .orElseThrow(() -> new ResourceNotFoundException("Transport agency not found with ID: " + request.transportAgencyId()));

        if (!agency.isActive()) {
            throw new InvalidOperationException("Transport agency '" + agency.getName() + "' is inactive and cannot accept dispatches.");
        }

        List<ShipmentEntity> shipmentsToDispatch = new ArrayList<>();
        for (UUID shipmentId : request.shipmentIds()) {
            ShipmentEntity shipment = shipmentRepository.findById(shipmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with ID: " + shipmentId));

            if (shipment.getPackedAt() == null) {
                throw new InvalidOperationException("Shipment " + shipment.getOrderNumber() + " cannot be dispatched because it has not been packed yet.");
            }

            if (shipment.getDailyDispatchBatch() != null) {
                throw new InvalidOperationException("Shipment " + shipment.getOrderNumber() + " is already assigned to dispatch batch: " + shipment.getDailyDispatchBatch().getBatchNumber());
            }

            if (shipment.getStatus() != ShipmentStatus.PROCESSING) {
                throw new InvalidOperationException("Shipment " + shipment.getOrderNumber() + " is in status " + shipment.getStatus() + " and cannot be added to a dispatch batch.");
            }

            shipmentsToDispatch.add(shipment);
        }

        String dateStr = request.dispatchDate().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String batchNumber = "DISPATCH-" + dateStr + "-" + suffix;

        DailyDispatchBatchEntity batch = new DailyDispatchBatchEntity(
                batchNumber,
                request.dispatchDate(),
                agency,
                staffId != null ? staffId : UUID.randomUUID(),
                shipmentsToDispatch.size(),
                request.billReference() != null ? request.billReference().trim() : null,
                request.notes() != null ? request.notes().trim() : null
        );

        DailyDispatchBatchEntity savedBatch = dailyDispatchBatchRepository.save(batch);

        for (ShipmentEntity shipment : shipmentsToDispatch) {
            shipment.setDailyDispatchBatch(savedBatch);
            shipment.setTransportAgency(agency);
            shipment.setUpdatedAt(Instant.now());
            shipmentRepository.save(shipment);
        }

        log.info("Created daily dispatch batch {} with {} shipments for agency {}",
                savedBatch.getBatchNumber(), savedBatch.getParcelCount(), agency.getName());

        emitOutboxEvent("DailyDispatchBatch", savedBatch.getId().toString(), "DISPATCH_BATCH_PREPARED", Map.of(
                "batchId", savedBatch.getId().toString(),
                "batchNumber", savedBatch.getBatchNumber(),
                "agencyId", agency.getId().toString(),
                "agencyName", agency.getName(),
                "parcelCount", savedBatch.getParcelCount()
        ));

        return mapToResponse(savedBatch);
    }

    public DailyDispatchBatchResponse recordAgencyHandover(UUID batchId, RecordHandoverRequest request, UUID staffId) {
        DailyDispatchBatchEntity batch = dailyDispatchBatchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispatch batch not found with ID: " + batchId));

        if (batch.getStatus() != DailyDispatchStatus.PREPARED) {
            throw new InvalidOperationException("Batch " + batch.getBatchNumber() + " cannot be handed over. Current status: " + batch.getStatus());
        }

        TransportAgencyEntity agency = transportAgencyRepository.findById(request.transportAgencyId())
                .orElseThrow(() -> new ResourceNotFoundException("Transport agency not found with ID: " + request.transportAgencyId()));

        Instant handoverTime = request.handoverAt() != null ? request.handoverAt() : Instant.now();
        batch.setTransportAgency(agency);
        batch.setStatus(DailyDispatchStatus.HANDED_OVER);
        batch.setHandoverAt(handoverTime);

        if (request.billReference() != null && !request.billReference().isBlank()) {
            batch.setBillReference(request.billReference().trim());
        }

        if (request.contactNotes() != null && !request.contactNotes().isBlank()) {
            String combinedNotes = (batch.getNotes() != null ? batch.getNotes() + "\n" : "") + "Handover note: " + request.contactNotes().trim();
            batch.setNotes(combinedNotes);
        }

        DailyDispatchBatchEntity savedBatch = dailyDispatchBatchRepository.save(batch);

        List<ShipmentEntity> shipments = shipmentRepository.findByDailyDispatchBatchId(batchId);
        for (ShipmentEntity s : shipments) {
            s.setStatus(ShipmentStatus.DISPATCHED);
            s.setDispatchDate(savedBatch.getDispatchDate());
            s.setDispatchedAt(handoverTime);
            s.setTransportAgency(agency);
            s.setUpdatedAt(Instant.now());
            shipmentRepository.save(s);

            emitOutboxEvent("Shipment", s.getId().toString(), "SHIPMENT_DISPATCHED", Map.of(
                    "shipmentId", s.getId().toString(),
                    "orderId", s.getOrderId().toString(),
                    "orderNumber", s.getOrderNumber(),
                    "dispatchDate", savedBatch.getDispatchDate().toString(),
                    "agencyName", agency.getName()
            ));
        }

        log.info("Batch {} handed over to agency {} with {} parcels by staff {}",
                savedBatch.getBatchNumber(), agency.getName(), savedBatch.getParcelCount(), staffId);

        emitOutboxEvent("DailyDispatchBatch", savedBatch.getId().toString(), "DISPATCH_BATCH_HANDED_OVER", Map.of(
                "batchId", savedBatch.getId().toString(),
                "batchNumber", savedBatch.getBatchNumber(),
                "agencyName", agency.getName(),
                "handoverAt", handoverTime.toString()
        ));

        return mapToResponse(savedBatch);
    }

    @Transactional(readOnly = true)
    public DailyDispatchBatchResponse getBatchById(UUID batchId) {
        DailyDispatchBatchEntity batch = dailyDispatchBatchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispatch batch not found with ID: " + batchId));
        return mapToResponse(batch);
    }

    @Transactional(readOnly = true)
    public List<DailyDispatchBatchResponse> getBatchesByDate(LocalDate date) {
        return dailyDispatchBatchRepository.findByDispatchDateOrderByCreatedAtDesc(date).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<DailyDispatchBatchResponse> getAllBatches(Pageable pageable) {
        Page<DailyDispatchBatchEntity> page = dailyDispatchBatchRepository.findAllByOrderByDispatchDateDesc(pageable);
        List<DailyDispatchBatchResponse> content = page.getContent().stream().map(this::mapToResponse).toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    private DailyDispatchBatchResponse mapToResponse(DailyDispatchBatchEntity entity) {
        UUID agencyId = entity.getTransportAgency() != null ? entity.getTransportAgency().getId() : null;
        String agencyName = entity.getTransportAgency() != null ? entity.getTransportAgency().getName() : null;

        return new DailyDispatchBatchResponse(
                entity.getId(),
                entity.getBatchNumber(),
                entity.getDispatchDate(),
                agencyId,
                agencyName,
                entity.getPreparedBy(),
                entity.getParcelCount(),
                entity.getBillReference(),
                entity.getStatus(),
                entity.getHandoverAt(),
                entity.getNotes(),
                entity.getCreatedAt()
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
