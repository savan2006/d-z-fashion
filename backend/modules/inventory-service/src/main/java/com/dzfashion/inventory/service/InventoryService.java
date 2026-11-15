package com.dzfashion.inventory.service;

import com.dzfashion.inventory.dto.request.InitializeStockRequest;
import com.dzfashion.inventory.dto.request.ManufactureStockRequest;
import com.dzfashion.inventory.dto.request.ReturnInspectionRequest;
import com.dzfashion.inventory.dto.request.StockAdjustmentRequest;
import com.dzfashion.inventory.dto.response.PageResponse;
import com.dzfashion.inventory.dto.response.StockResponse;
import com.dzfashion.inventory.entity.WarehouseEntity;
import com.dzfashion.inventory.entity.WarehouseStockEntity;
import com.dzfashion.inventory.enums.ItemCondition;
import com.dzfashion.inventory.enums.StockMovementReason;
import com.dzfashion.inventory.exception.InvalidOperationException;
import com.dzfashion.inventory.exception.ResourceConflictException;
import com.dzfashion.inventory.exception.ResourceNotFoundException;
import com.dzfashion.inventory.repository.WarehouseStockRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    private final WarehouseStockRepository stockRepository;
    private final WarehouseService warehouseService;
    private final StockMovementService movementService;
    private final AlertNotificationService alertService;

    public InventoryService(WarehouseStockRepository stockRepository,
                            WarehouseService warehouseService,
                            StockMovementService movementService,
                            AlertNotificationService alertService) {
        this.stockRepository = stockRepository;
        this.warehouseService = warehouseService;
        this.movementService = movementService;
        this.alertService = alertService;
    }

    @Transactional
    public StockResponse initializeStock(InitializeStockRequest request, UUID actorId) {
        if (stockRepository.existsByVariantId(request.variantId())) {
            throw new ResourceConflictException("Stock already initialized for variant: " + request.variantId());
        }

        WarehouseEntity warehouse = warehouseService.getOrCreateSuratWarehouse();
        WarehouseStockEntity entity = new WarehouseStockEntity(
                warehouse,
                request.variantId(),
                request.sku(),
                request.initialQuantity(),
                request.lowStockThreshold()
        );

        WarehouseStockEntity saved = stockRepository.save(entity);

        if (request.initialQuantity() > 0) {
            movementService.recordMovement(
                    saved.getVariantId(),
                    request.initialQuantity(),
                    saved.getAvailableQty(),
                    StockMovementReason.NEW_MANUFACTURE,
                    "INIT",
                    saved.getSku(),
                    actorId
            );
        }

        alertService.evaluateStockLevel(saved);

        return mapToResponse(saved);
    }

    @Transactional
    public StockResponse addManufacturedStock(ManufactureStockRequest request, UUID actorId) {
        WarehouseStockEntity stock = stockRepository.findByVariantIdWithLock(request.variantId())
                .orElseThrow(() -> new ResourceNotFoundException("Stock record not found for variant ID: " + request.variantId()));

        stock.setPhysicalQty(stock.getPhysicalQty() + request.quantity());
        WarehouseStockEntity saved = stockRepository.save(stock);

        movementService.recordMovement(
                saved.getVariantId(),
                request.quantity(),
                saved.getAvailableQty(),
                StockMovementReason.NEW_MANUFACTURE,
                "MANUFACTURE",
                request.notes() != null ? request.notes() : "Batch manufacture",
                actorId
        );

        alertService.evaluateStockLevel(saved);

        return mapToResponse(saved);
    }

    @Transactional
    public StockResponse adjustStock(StockAdjustmentRequest request, UUID actorId) {
        WarehouseStockEntity stock = stockRepository.findByVariantIdWithLock(request.variantId())
                .orElseThrow(() -> new ResourceNotFoundException("Stock record not found for variant ID: " + request.variantId()));

        int newPhysical = stock.getPhysicalQty() + request.quantityDelta();
        if (newPhysical < 0) {
            throw new InvalidOperationException("Stock adjustment would result in negative physical quantity: " + newPhysical);
        }

        int newAvailable = newPhysical - stock.getReservedQty();
        if (newAvailable < 0) {
            throw new InvalidOperationException(String.format(
                    "Stock adjustment would cause available stock to drop below zero. Reserved: %d, Proposed physical: %d",
                    stock.getReservedQty(), newPhysical
            ));
        }

        stock.setPhysicalQty(newPhysical);
        WarehouseStockEntity saved = stockRepository.save(stock);

        movementService.recordMovement(
                saved.getVariantId(),
                request.quantityDelta(),
                saved.getAvailableQty(),
                request.reason() != null ? request.reason() : StockMovementReason.MANUAL_CORRECTION,
                "ADJUSTMENT",
                request.notes(),
                actorId
        );

        alertService.evaluateStockLevel(saved);

        return mapToResponse(saved);
    }

    @Transactional
    public StockResponse inspectAndRestockReturnedItem(ReturnInspectionRequest request, UUID actorId) {
        WarehouseStockEntity stock = stockRepository.findByVariantIdWithLock(request.variantId())
                .orElseThrow(() -> new ResourceNotFoundException("Stock record not found for variant ID: " + request.variantId()));

        if (request.condition() == ItemCondition.RESELLABLE) {
            // Re-sellable item: add back to physical stock
            stock.setPhysicalQty(stock.getPhysicalQty() + request.quantity());
            stockRepository.save(stock);

            movementService.recordMovement(
                    stock.getVariantId(),
                    request.quantity(),
                    stock.getAvailableQty(),
                    StockMovementReason.RETURN_RESTOCK,
                    "RETURN_ORDER",
                    request.returnOrderId() != null ? request.returnOrderId() : "Inspection passed",
                    actorId
            );

            alertService.evaluateStockLevel(stock);
        } else {
            // Damaged item: isolate into quarantine, MUST NOT become sellable
            stock.setQuarantinedQty(stock.getQuarantinedQty() + request.quantity());
            stockRepository.save(stock);

            movementService.recordMovement(
                    stock.getVariantId(),
                    request.quantity(),
                    stock.getAvailableQty(),
                    StockMovementReason.DAMAGE_QUARANTINE,
                    "RETURN_ORDER_DAMAGED",
                    request.notes() != null ? request.notes() : "Damaged return quarantined",
                    actorId
            );
        }

        return mapToResponse(stock);
    }

    @Transactional
    public StockResponse updateThreshold(UUID variantId, int threshold, UUID ownerId) {
        WarehouseStockEntity stock = stockRepository.findByVariantId(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Stock record not found for variant ID: " + variantId));

        stock.setLowStockThreshold(Math.max(0, threshold));
        WarehouseStockEntity saved = stockRepository.save(stock);

        alertService.evaluateStockLevel(saved);

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public StockResponse getStockByVariantId(UUID variantId) {
        WarehouseStockEntity stock = stockRepository.findByVariantId(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Stock record not found for variant ID: " + variantId));
        return mapToResponse(stock);
    }

    @Transactional(readOnly = true)
    public StockResponse getStockBySku(String sku) {
        WarehouseStockEntity stock = stockRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Stock record not found for SKU: " + sku));
        return mapToResponse(stock);
    }

    @Transactional(readOnly = true)
    public PageResponse<StockResponse> getLowStockQueue(Pageable pageable) {
        Page<WarehouseStockEntity> page = stockRepository.findLowStockVariants(pageable);
        List<StockResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    public StockResponse mapToResponse(WarehouseStockEntity entity) {
        return new StockResponse(
                entity.getVariantId(),
                entity.getSku(),
                entity.getPhysicalQty(),
                entity.getReservedQty(),
                entity.getAvailableQty(),
                entity.getQuarantinedQty(),
                entity.getLowStockThreshold(),
                entity.getUpdatedAt()
        );
    }
}
