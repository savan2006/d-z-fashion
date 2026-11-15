package com.dzfashion.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "warehouse_stock", schema = "inventory")
public class WarehouseStockEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private WarehouseEntity warehouse;

    @Column(name = "variant_id", nullable = false, unique = true)
    private UUID variantId;

    @Column(name = "sku", nullable = false, unique = true, length = 60)
    private String sku;

    @Column(name = "physical_qty", nullable = false)
    private int physicalQty = 0;

    @Column(name = "reserved_qty", nullable = false)
    private int reservedQty = 0;

    @Column(name = "available_qty", insertable = false, updatable = false)
    private int availableQty = 0;

    @Column(name = "quarantined_qty", nullable = false)
    private int quarantinedQty = 0;

    @Column(name = "low_stock_threshold", nullable = false)
    private int lowStockThreshold = 5;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public WarehouseStockEntity() {
    }

    public WarehouseStockEntity(WarehouseEntity warehouse, UUID variantId, String sku, int physicalQty, int lowStockThreshold) {
        this.warehouse = warehouse;
        this.variantId = variantId;
        this.sku = sku;
        this.physicalQty = Math.max(0, physicalQty);
        this.reservedQty = 0;
        this.quarantinedQty = 0;
        this.lowStockThreshold = Math.max(0, lowStockThreshold);
        this.availableQty = this.physicalQty;
    }

    @PrePersist
    protected void onCreate() {
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public int getAvailableQty() {
        return physicalQty - reservedQty;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public WarehouseEntity getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(WarehouseEntity warehouse) {
        this.warehouse = warehouse;
    }

    public UUID getVariantId() {
        return variantId;
    }

    public void setVariantId(UUID variantId) {
        this.variantId = variantId;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public int getPhysicalQty() {
        return physicalQty;
    }

    public void setPhysicalQty(int physicalQty) {
        this.physicalQty = physicalQty;
    }

    public int getReservedQty() {
        return reservedQty;
    }

    public void setReservedQty(int reservedQty) {
        this.reservedQty = reservedQty;
    }

    public int getQuarantinedQty() {
        return quarantinedQty;
    }

    public void setQuarantinedQty(int quarantinedQty) {
        this.quarantinedQty = quarantinedQty;
    }

    public int getLowStockThreshold() {
        return lowStockThreshold;
    }

    public void setLowStockThreshold(int lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
