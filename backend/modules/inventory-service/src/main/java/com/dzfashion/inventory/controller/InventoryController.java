package com.dzfashion.inventory.controller;

import com.dzfashion.inventory.dto.response.StockResponse;
import com.dzfashion.inventory.service.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/variants/{variantId}")
    public ResponseEntity<StockResponse> getStockByVariantId(@PathVariable UUID variantId) {
        StockResponse stock = inventoryService.getStockByVariantId(variantId);
        return ResponseEntity.ok(stock);
    }

    @GetMapping("/sku/{sku}")
    public ResponseEntity<StockResponse> getStockBySku(@PathVariable String sku) {
        StockResponse stock = inventoryService.getStockBySku(sku);
        return ResponseEntity.ok(stock);
    }
}
