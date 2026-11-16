package com.dzfashion.fulfillment.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "shipment_items")
public class ShipmentItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private ShipmentEntity shipment;

    @Column(name = "order_item_id", nullable = false)
    private UUID orderItemId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "variant_id", nullable = false)
    private UUID variantId;

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(name = "sku", nullable = false, length = 60)
    private String sku;

    @Column(name = "size", length = 20)
    private String size;

    @Column(name = "color", length = 50)
    private String color;

    @Column(name = "bin_location", length = 50)
    private String binLocation = "SURAT-WH-MAIN";

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "picked_quantity", nullable = false)
    private int pickedQuantity = 0;

    @Column(name = "is_picked", nullable = false)
    private boolean picked = false;

    @Column(name = "is_packed", nullable = false)
    private boolean packed = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public ShipmentItemEntity() {
    }

    public ShipmentItemEntity(UUID orderItemId, UUID productId, UUID variantId, String productName, String sku, String size, String color, String binLocation, int quantity) {
        this.orderItemId = orderItemId;
        this.productId = productId != null ? productId : UUID.randomUUID();
        this.variantId = variantId != null ? variantId : UUID.randomUUID();
        this.productName = productName;
        this.sku = sku;
        this.size = size;
        this.color = color;
        this.binLocation = binLocation != null ? binLocation : "SURAT-WH-MAIN";
        this.quantity = quantity;
        this.pickedQuantity = 0;
        this.picked = false;
        this.packed = false;
        this.createdAt = Instant.now();
    }

    public ShipmentItemEntity(UUID orderItemId, String sku, String productName, String size, String color, String binLocation, int quantity) {
        this(orderItemId, UUID.randomUUID(), UUID.randomUUID(), productName, sku, size, color, binLocation, quantity);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ShipmentEntity getShipment() {
        return shipment;
    }

    public void setShipment(ShipmentEntity shipment) {
        this.shipment = shipment;
    }

    public UUID getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(UUID orderItemId) {
        this.orderItemId = orderItemId;
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public UUID getVariantId() {
        return variantId;
    }

    public void setVariantId(UUID variantId) {
        this.variantId = variantId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getBinLocation() {
        return binLocation;
    }

    public void setBinLocation(String binLocation) {
        this.binLocation = binLocation;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getPickedQuantity() {
        return pickedQuantity;
    }

    public void setPickedQuantity(int pickedQuantity) {
        this.pickedQuantity = pickedQuantity;
    }

    public boolean isPicked() {
        return picked;
    }

    public void setPicked(boolean picked) {
        this.picked = picked;
    }

    public boolean isPacked() {
        return packed;
    }

    public void setPacked(boolean packed) {
        this.packed = packed;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
