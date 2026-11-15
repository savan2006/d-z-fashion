package com.dzfashion.inventory.service;

import com.dzfashion.inventory.dto.request.InitializeStockRequest;
import com.dzfashion.inventory.dto.request.ManufactureStockRequest;
import com.dzfashion.inventory.dto.request.ReturnInspectionRequest;
import com.dzfashion.inventory.dto.request.StockAdjustmentRequest;
import com.dzfashion.inventory.dto.response.StockResponse;
import com.dzfashion.inventory.entity.WarehouseEntity;
import com.dzfashion.inventory.entity.WarehouseStockEntity;
import com.dzfashion.inventory.enums.ItemCondition;
import com.dzfashion.inventory.enums.StockMovementReason;
import com.dzfashion.inventory.exception.InvalidOperationException;
import com.dzfashion.inventory.exception.ResourceConflictException;
import com.dzfashion.inventory.repository.WarehouseStockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private WarehouseStockRepository stockRepository;

    @Mock
    private WarehouseService warehouseService;

    @Mock
    private StockMovementService movementService;

    @Mock
    private AlertNotificationService alertService;

    @InjectMocks
    private InventoryService inventoryService;

    private UUID variantId;
    private UUID actorId;
    private WarehouseEntity suratWarehouse;
    private WarehouseStockEntity stockEntity;

    @BeforeEach
    void setUp() {
        variantId = UUID.randomUUID();
        actorId = UUID.randomUUID();

        suratWarehouse = new WarehouseEntity("SURAT-MAIN", "Surat Central Warehouse", "Surat", "Gujarat", "India");
        suratWarehouse.setId(UUID.randomUUID());

        stockEntity = new WarehouseStockEntity(suratWarehouse, variantId, "DZF-TEST-SKU", 20, 5);
        stockEntity.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Initialize stock for new variant creates record and movement")
    void initializeStockSuccess() {
        InitializeStockRequest request = new InitializeStockRequest(variantId, "DZF-TEST-SKU", 15, 5);

        when(stockRepository.existsByVariantId(variantId)).thenReturn(false);
        when(warehouseService.getOrCreateSuratWarehouse()).thenReturn(suratWarehouse);
        when(stockRepository.save(any(WarehouseStockEntity.class))).thenAnswer(inv -> {
            WarehouseStockEntity entity = inv.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        StockResponse response = inventoryService.initializeStock(request, actorId);

        assertThat(response).isNotNull();
        assertThat(response.physicalQty()).isEqualTo(15);
        assertThat(response.availableQty()).isEqualTo(15);
        assertThat(response.lowStockThreshold()).isEqualTo(5);

        verify(movementService).recordMovement(eq(variantId), eq(15), eq(15), eq(StockMovementReason.NEW_MANUFACTURE), anyString(), anyString(), eq(actorId));
        verify(alertService).evaluateStockLevel(any());
    }

    @Test
    @DisplayName("Initialize stock throws ResourceConflictException on existing variant stock")
    void initializeStockConflict() {
        InitializeStockRequest request = new InitializeStockRequest(variantId, "DZF-TEST-SKU", 10, 5);
        when(stockRepository.existsByVariantId(variantId)).thenReturn(true);

        assertThatThrownBy(() -> inventoryService.initializeStock(request, actorId))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Stock already initialized");
    }

    @Test
    @DisplayName("Manager adds newly manufactured stock: increases physical quantity")
    void addManufacturedStockSuccess() {
        ManufactureStockRequest request = new ManufactureStockRequest(variantId, 25, "Fresh batch ready");

        when(stockRepository.findByVariantIdWithLock(variantId)).thenReturn(Optional.of(stockEntity));
        when(stockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        StockResponse response = inventoryService.addManufacturedStock(request, actorId);

        assertThat(response.physicalQty()).isEqualTo(45);
        assertThat(response.availableQty()).isEqualTo(45);
        verify(movementService).recordMovement(eq(variantId), eq(25), eq(45), eq(StockMovementReason.NEW_MANUFACTURE), anyString(), eq("Fresh batch ready"), eq(actorId));
    }

    @Test
    @DisplayName("Stock adjustment applies valid delta and records audit movement")
    void adjustStockSuccess() {
        StockAdjustmentRequest request = new StockAdjustmentRequest(
                variantId, -5, StockMovementReason.MANUAL_CORRECTION, "Audit correction"
        );

        when(stockRepository.findByVariantIdWithLock(variantId)).thenReturn(Optional.of(stockEntity));
        when(stockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        StockResponse response = inventoryService.adjustStock(request, actorId);

        assertThat(response.physicalQty()).isEqualTo(15);
        assertThat(response.availableQty()).isEqualTo(15);
        verify(movementService).recordMovement(eq(variantId), eq(-5), eq(15), eq(StockMovementReason.MANUAL_CORRECTION), anyString(), eq("Audit correction"), eq(actorId));
    }

    @Test
    @DisplayName("Stock adjustment fails when available stock would become negative")
    void adjustStockNegativeAvailableFails() {
        stockEntity.setPhysicalQty(10);
        stockEntity.setReservedQty(8); // Available = 2

        StockAdjustmentRequest request = new StockAdjustmentRequest(
                variantId, -5, StockMovementReason.MANUAL_CORRECTION, "Too large deduction"
        );

        when(stockRepository.findByVariantIdWithLock(variantId)).thenReturn(Optional.of(stockEntity));

        assertThatThrownBy(() -> inventoryService.adjustStock(request, actorId))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("would cause available stock to drop below zero");
    }

    @Test
    @DisplayName("Returned item inspection: RESELLABLE condition restores physical stock")
    void returnInspectionResellableRestoresStock() {
        ReturnInspectionRequest request = new ReturnInspectionRequest(
                variantId, 2, ItemCondition.RESELLABLE, "RET-1001", "Unworn with tags"
        );

        when(stockRepository.findByVariantIdWithLock(variantId)).thenReturn(Optional.of(stockEntity));
        when(stockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        StockResponse response = inventoryService.inspectAndRestockReturnedItem(request, actorId);

        assertThat(response.physicalQty()).isEqualTo(22);
        assertThat(response.availableQty()).isEqualTo(22);
        assertThat(response.quarantinedQty()).isEqualTo(0);
        verify(movementService).recordMovement(eq(variantId), eq(2), eq(22), eq(StockMovementReason.RETURN_RESTOCK), anyString(), anyString(), eq(actorId));
    }

    @Test
    @DisplayName("Returned item inspection: DAMAGED condition increments quarantine and DOES NOT increase sellable stock")
    void returnInspectionDamagedQuarantinesStock() {
        ReturnInspectionRequest request = new ReturnInspectionRequest(
                variantId, 2, ItemCondition.DAMAGED, "RET-1002", "Torn seam"
        );

        when(stockRepository.findByVariantIdWithLock(variantId)).thenReturn(Optional.of(stockEntity));
        when(stockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        StockResponse response = inventoryService.inspectAndRestockReturnedItem(request, actorId);

        assertThat(response.physicalQty()).isEqualTo(20); // Unchanged!
        assertThat(response.availableQty()).isEqualTo(20); // Unchanged!
        assertThat(response.quarantinedQty()).isEqualTo(2); // Isolated in quarantine!
        verify(movementService).recordMovement(eq(variantId), eq(2), eq(20), eq(StockMovementReason.DAMAGE_QUARANTINE), anyString(), anyString(), eq(actorId));
    }

    @Test
    @DisplayName("Owner updates low stock threshold")
    void updateThresholdSuccess() {
        when(stockRepository.findByVariantId(variantId)).thenReturn(Optional.of(stockEntity));
        when(stockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        StockResponse response = inventoryService.updateThreshold(variantId, 12, actorId);

        assertThat(response.lowStockThreshold()).isEqualTo(12);
        verify(alertService).evaluateStockLevel(stockEntity);
    }
}
