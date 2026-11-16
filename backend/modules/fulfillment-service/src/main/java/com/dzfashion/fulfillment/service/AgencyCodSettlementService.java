package com.dzfashion.fulfillment.service;

import com.dzfashion.fulfillment.dto.request.CreateAgencyCodSettlementRequest;
import com.dzfashion.fulfillment.dto.response.AgencyCodSettlementResponse;
import com.dzfashion.fulfillment.dto.response.PageResponse;
import com.dzfashion.fulfillment.dto.response.ShipmentResponse;
import com.dzfashion.fulfillment.entity.AgencyCodSettlementEntity;
import com.dzfashion.fulfillment.entity.OutboxEventEntity;
import com.dzfashion.fulfillment.entity.ShipmentEntity;
import com.dzfashion.fulfillment.entity.TransportAgencyEntity;
import com.dzfashion.fulfillment.enums.AgencyCodSettlementStatus;
import com.dzfashion.fulfillment.enums.ShipmentStatus;
import com.dzfashion.fulfillment.exception.InvalidOperationException;
import com.dzfashion.fulfillment.exception.ResourceNotFoundException;
import com.dzfashion.fulfillment.repository.AgencyCodSettlementRepository;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class AgencyCodSettlementService {

    private static final Logger log = LoggerFactory.getLogger(AgencyCodSettlementService.class);

    private final AgencyCodSettlementRepository agencyCodSettlementRepository;
    private final ShipmentRepository shipmentRepository;
    private final TransportAgencyRepository transportAgencyRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ShipmentService shipmentService;
    private final ObjectMapper objectMapper;

    public AgencyCodSettlementService(AgencyCodSettlementRepository agencyCodSettlementRepository,
                                      ShipmentRepository shipmentRepository,
                                      TransportAgencyRepository transportAgencyRepository,
                                      OutboxEventRepository outboxEventRepository,
                                      ShipmentService shipmentService,
                                      ObjectMapper objectMapper) {
        this.agencyCodSettlementRepository = agencyCodSettlementRepository;
        this.shipmentRepository = shipmentRepository;
        this.transportAgencyRepository = transportAgencyRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.shipmentService = shipmentService;
        this.objectMapper = objectMapper;
    }

    public AgencyCodSettlementResponse prepareSettlement(CreateAgencyCodSettlementRequest request, UUID managerId) {
        TransportAgencyEntity agency = transportAgencyRepository.findById(request.transportAgencyId())
                .orElseThrow(() -> new ResourceNotFoundException("Transport agency not found with ID: " + request.transportAgencyId()));

        List<ShipmentEntity> shipments = new ArrayList<>();
        BigDecimal totalExpectedCod = BigDecimal.ZERO;

        for (UUID shipmentId : request.shipmentIds()) {
            ShipmentEntity shipment = shipmentRepository.findById(shipmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with ID: " + shipmentId));

            if (shipment.getTransportAgency() == null || !shipment.getTransportAgency().getId().equals(agency.getId())) {
                throw new InvalidOperationException("Shipment " + shipment.getOrderNumber() + " does not belong to transport agency: " + agency.getName());
            }

            if (shipment.getStatus() != ShipmentStatus.DELIVERED) {
                throw new InvalidOperationException("Shipment " + shipment.getOrderNumber() + " is in status " + shipment.getStatus() + " and cannot be settled. Only DELIVERED shipments can be settled.");
            }

            if (!shipment.isCod()) {
                throw new InvalidOperationException("Shipment " + shipment.getOrderNumber() + " is not a COD shipment.");
            }

            if (shipment.getCodSettlementBatchId() != null) {
                throw new InvalidOperationException("Shipment " + shipment.getOrderNumber() + " is already included in COD settlement batch: " + shipment.getCodSettlementBatchId());
            }

            BigDecimal collected = shipment.getCodCollectedAmount() != null ? shipment.getCodCollectedAmount() : shipment.getCodAmount();
            totalExpectedCod = totalExpectedCod.add(collected);
            shipments.add(shipment);
        }

        BigDecimal remitted = request.remittedAmount();
        BigDecimal discrepancy = remitted.subtract(totalExpectedCod);

        String dateStr = request.settlementDate().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String batchRef = "COD-SETTLE-" + dateStr + "-" + suffix;

        AgencyCodSettlementEntity settlement = new AgencyCodSettlementEntity(
                batchRef,
                agency,
                request.settlementDate(),
                shipments.size(),
                totalExpectedCod,
                remitted,
                discrepancy,
                managerId != null ? managerId : UUID.randomUUID(),
                request.proofReference() != null ? request.proofReference().trim() : null,
                request.notes() != null ? request.notes().trim() : null
        );

        AgencyCodSettlementEntity savedSettlement = agencyCodSettlementRepository.save(settlement);

        for (ShipmentEntity s : shipments) {
            s.setCodSettlementBatchId(savedSettlement.getId());
            s.setUpdatedAt(Instant.now());
            shipmentRepository.save(s);
        }

        log.info("Prepared COD settlement {} for agency {} with {} shipments. Expected: {}, Remitted: {}, Discrepancy: {}",
                savedSettlement.getBatchReference(), agency.getName(), shipments.size(), totalExpectedCod, remitted, discrepancy);

        emitOutboxEvent("AgencyCodSettlement", savedSettlement.getId().toString(), "AGENCY_COD_SETTLEMENT_PREPARED", Map.of(
                "settlementId", savedSettlement.getId().toString(),
                "batchReference", savedSettlement.getBatchReference(),
                "agencyName", agency.getName(),
                "totalExpectedCod", totalExpectedCod.toString(),
                "remittedAmount", remitted.toString(),
                "discrepancyAmount", discrepancy.toString()
        ));

        return mapToResponse(savedSettlement);
    }

    public AgencyCodSettlementResponse approveSettlement(UUID settlementId, UUID ownerId) {
        AgencyCodSettlementEntity settlement = agencyCodSettlementRepository.findById(settlementId)
                .orElseThrow(() -> new ResourceNotFoundException("COD settlement batch not found with ID: " + settlementId));

        if (settlement.getStatus() == AgencyCodSettlementStatus.APPROVED) {
            throw new InvalidOperationException("COD settlement batch " + settlement.getBatchReference() + " is already approved.");
        }

        settlement.setStatus(AgencyCodSettlementStatus.APPROVED);
        settlement.setApprovedBy(ownerId != null ? ownerId : UUID.randomUUID());
        settlement.setApprovedAt(Instant.now());

        AgencyCodSettlementEntity saved = agencyCodSettlementRepository.save(settlement);
        log.info("COD settlement {} approved by Owner {}", saved.getBatchReference(), ownerId);

        emitOutboxEvent("AgencyCodSettlement", saved.getId().toString(), "AGENCY_COD_SETTLEMENT_APPROVED", Map.of(
                "settlementId", saved.getId().toString(),
                "batchReference", saved.getBatchReference(),
                "approvedBy", saved.getApprovedBy().toString(),
                "approvedAt", saved.getApprovedAt().toString()
        ));

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public AgencyCodSettlementResponse getSettlementById(UUID settlementId) {
        AgencyCodSettlementEntity settlement = agencyCodSettlementRepository.findById(settlementId)
                .orElseThrow(() -> new ResourceNotFoundException("COD settlement batch not found with ID: " + settlementId));
        return mapToResponse(settlement);
    }

    @Transactional(readOnly = true)
    public List<AgencyCodSettlementResponse> getSettlementsByAgency(UUID agencyId) {
        return agencyCodSettlementRepository.findByTransportAgencyIdOrderBySettlementDateDesc(agencyId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ShipmentResponse> getEligibleShipmentsForAgency(UUID agencyId) {
        return shipmentRepository.findDeliveredCodEligibleForSettlement(agencyId).stream()
                .map(shipmentService::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<AgencyCodSettlementResponse> getAllSettlements(Pageable pageable) {
        Page<AgencyCodSettlementEntity> page = agencyCodSettlementRepository.findAllByOrderBySettlementDateDesc(pageable);
        List<AgencyCodSettlementResponse> content = page.getContent().stream().map(this::mapToResponse).toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    private AgencyCodSettlementResponse mapToResponse(AgencyCodSettlementEntity entity) {
        return new AgencyCodSettlementResponse(
                entity.getId(),
                entity.getBatchReference(),
                entity.getTransportAgency().getId(),
                entity.getTransportAgency().getName(),
                entity.getSettlementDate(),
                entity.getTotalDeliveredOrders(),
                entity.getTotalExpectedCod(),
                entity.getRemittedAmount(),
                entity.getDiscrepancyAmount(),
                entity.getStatus(),
                entity.getPreparedBy(),
                entity.getApprovedBy(),
                entity.getProofReference(),
                entity.getNotes(),
                entity.getCreatedAt(),
                entity.getApprovedAt()
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
