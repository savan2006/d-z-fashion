package com.dzfashion.fulfillment.service;

import com.dzfashion.fulfillment.config.FulfillmentProperties;
import com.dzfashion.fulfillment.dto.document.DailyPickingListDocument;
import com.dzfashion.fulfillment.dto.document.DispatchManifestDocument;
import com.dzfashion.fulfillment.dto.document.PackingSlipDocument;
import com.dzfashion.fulfillment.entity.DailyDispatchBatchEntity;
import com.dzfashion.fulfillment.entity.ShipmentEntity;
import com.dzfashion.fulfillment.entity.ShipmentItemEntity;
import com.dzfashion.fulfillment.enums.ShipmentStatus;
import com.dzfashion.fulfillment.exception.ResourceNotFoundException;
import com.dzfashion.fulfillment.repository.DailyDispatchBatchRepository;
import com.dzfashion.fulfillment.repository.ShipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class FulfillmentDocumentService {

    private final ShipmentRepository shipmentRepository;
    private final DailyDispatchBatchRepository dailyDispatchBatchRepository;
    private final FulfillmentProperties fulfillmentProperties;

    public FulfillmentDocumentService(ShipmentRepository shipmentRepository,
                                      DailyDispatchBatchRepository dailyDispatchBatchRepository,
                                      FulfillmentProperties fulfillmentProperties) {
        this.shipmentRepository = shipmentRepository;
        this.dailyDispatchBatchRepository = dailyDispatchBatchRepository;
        this.fulfillmentProperties = fulfillmentProperties;
    }

    public DailyPickingListDocument generateCombinedPickingList() {
        List<ShipmentEntity> processingShipments = shipmentRepository.findByStatus(ShipmentStatus.PROCESSING);

        // Group items across orders by SKU
        Map<String, SkuAccumulator> skuMap = new LinkedHashMap<>();
        int totalUnits = 0;

        for (ShipmentEntity shipment : processingShipments) {
            for (ShipmentItemEntity item : shipment.getItems()) {
                String sku = item.getSku();
                SkuAccumulator acc = skuMap.computeIfAbsent(sku, k -> new SkuAccumulator(
                        item.getSku(),
                        item.getProductName(),
                        item.getSize(),
                        item.getColor(),
                        item.getBinLocation()
                ));
                acc.addOrderAllocation(shipment.getOrderNumber(), item.getQuantity());
                totalUnits += item.getQuantity();
            }
        }

        List<DailyPickingListDocument.SkuPickSummary> items = skuMap.values().stream()
                .map(SkuAccumulator::toSkuPickSummary)
                .toList();

        return new DailyPickingListDocument(
                fulfillmentProperties.getWarehouseName(),
                Instant.now(),
                processingShipments.size(),
                totalUnits,
                items
        );
    }

    public PackingSlipDocument generatePackingSlip(UUID orderId) {
        ShipmentEntity shipment = shipmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found for order ID: " + orderId));

        String maskedPhone = maskPhoneNumber(shipment.getRecipientPhone());

        List<PackingSlipDocument.PackingSlipItem> items = shipment.getItems().stream()
                .map(item -> new PackingSlipDocument.PackingSlipItem(
                        item.getSku(),
                        item.getProductName(),
                        item.getSize(),
                        item.getColor(),
                        item.getQuantity()
                )).toList();

        return new PackingSlipDocument(
                shipment.getOrderNumber(),
                fulfillmentProperties.getWarehouseName(),
                Instant.now(),
                shipment.getRecipientName(),
                maskedPhone,
                shipment.getShippingAddress(),
                shipment.getCity(),
                shipment.getState(),
                shipment.getPincode(),
                shipment.isCod(),
                shipment.getCodAmount(),
                items
        );
    }

    public DispatchManifestDocument generateDispatchManifest(UUID batchId) {
        DailyDispatchBatchEntity batch = dailyDispatchBatchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispatch batch not found with ID: " + batchId));

        List<ShipmentEntity> shipments = shipmentRepository.findByDailyDispatchBatchId(batchId);

        BigDecimal totalCod = BigDecimal.ZERO;
        List<DispatchManifestDocument.ManifestParcel> parcels = new ArrayList<>();

        for (ShipmentEntity s : shipments) {
            parcels.add(new DispatchManifestDocument.ManifestParcel(
                    s.getOrderNumber(),
                    s.getRecipientName(),
                    s.getCity(),
                    s.getState(),
                    s.getPincode(),
                    s.isCod(),
                    s.getCodAmount()
            ));
            if (s.isCod() && s.getCodAmount() != null) {
                totalCod = totalCod.add(s.getCodAmount());
            }
        }

        return new DispatchManifestDocument(
                batch.getBatchNumber(),
                batch.getDispatchDate(),
                Instant.now(),
                batch.getTransportAgency().getName(),
                fulfillmentProperties.getWarehouseName(),
                parcels.size(),
                totalCod,
                batch.getBillReference(),
                parcels
        );
    }

    private String maskPhoneNumber(String phone) {
        if (phone == null || phone.isBlank()) {
            return "****";
        }
        String clean = phone.trim();
        if (clean.length() <= 4) {
            return "****";
        }
        if (clean.length() >= 10) {
            // Show first 5 and last 1 or 2 digits
            return clean.substring(0, 5) + "****" + clean.substring(clean.length() - 1);
        }
        return clean.substring(0, 2) + "****" + clean.substring(clean.length() - 1);
    }

    private static class SkuAccumulator {
        private final String sku;
        private final String productName;
        private final String size;
        private final String color;
        private final String binLocation;
        private int totalQuantity = 0;
        private final List<DailyPickingListDocument.OrderAllocation> allocations = new ArrayList<>();

        public SkuAccumulator(String sku, String productName, String size, String color, String binLocation) {
            this.sku = sku;
            this.productName = productName;
            this.size = size;
            this.color = color;
            this.binLocation = binLocation;
        }

        public void addOrderAllocation(String orderNumber, int quantity) {
            this.totalQuantity += quantity;
            this.allocations.add(new DailyPickingListDocument.OrderAllocation(orderNumber, quantity));
        }

        public DailyPickingListDocument.SkuPickSummary toSkuPickSummary() {
            return new DailyPickingListDocument.SkuPickSummary(
                    sku, productName, size, color, binLocation, totalQuantity, allocations
            );
        }
    }
}
