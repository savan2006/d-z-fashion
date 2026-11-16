package com.dzfashion.fulfillment.entity;

import com.dzfashion.fulfillment.enums.ReturnReason;
import com.dzfashion.fulfillment.enums.ReturnRequestStatus;
import com.dzfashion.fulfillment.enums.ReturnResolutionType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "return_requests")
public class ReturnRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "return_number", nullable = false, unique = true, length = 64)
    private String returnNumber;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "order_number", nullable = false, length = 64)
    private String orderNumber;

    @Column(name = "order_item_id", nullable = false)
    private UUID orderItemId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "sku", nullable = false, length = 60)
    private String sku;

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "original_unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal originalUnitPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 30)
    private ReturnReason reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_type", nullable = false, length = 30)
    private ReturnResolutionType resolutionType = ReturnResolutionType.EXCHANGE;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 35)
    private ReturnRequestStatus status = ReturnRequestStatus.REQUESTED;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "photo_urls", columnDefinition = "text[]")
    private List<String> photoUrls = new ArrayList<>();

    @Column(name = "customer_notes", columnDefinition = "TEXT")
    private String customerNotes;

    // Transport pickup details
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transport_agency_id")
    private TransportAgencyEntity transportAgency;

    @Column(name = "pickup_reference", length = 100)
    private String pickupReference;

    @Column(name = "pickup_scheduled_date")
    private LocalDate pickupScheduledDate;

    @Column(name = "picked_up_at")
    private Instant pickedUpAt;

    @Column(name = "received_at_warehouse_at")
    private Instant receivedAtWarehouseAt;

    // Exchange details
    @Column(name = "replacement_product_id")
    private UUID replacementProductId;

    @Column(name = "replacement_variant_id")
    private UUID replacementVariantId;

    @Column(name = "replacement_sku", length = 60)
    private String replacementSku;

    @Column(name = "replacement_product_name", length = 255)
    private String replacementProductName;

    @Column(name = "replacement_unit_price", precision = 12, scale = 2)
    private BigDecimal replacementUnitPrice;

    @Column(name = "price_difference", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceDifference = BigDecimal.ZERO;

    @Column(name = "price_difference_settled", nullable = false)
    private boolean priceDifferenceSettled = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replacement_shipment_id")
    private ShipmentEntity replacementShipment;

    // Refund details (Owner approval required)
    @Column(name = "refund_amount", precision = 12, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "refund_deadline")
    private Instant refundDeadline;

    @Column(name = "refund_approved_by")
    private UUID refundApprovedBy;

    @Column(name = "refund_approved_at")
    private Instant refundApprovedAt;

    @Column(name = "refund_rejection_reason", columnDefinition = "TEXT")
    private String refundRejectionReason;

    @Column(name = "is_cod_refund", nullable = false)
    private boolean codRefund = false;

    @OneToOne(mappedBy = "returnRequest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ReturnInspectionEntity inspection;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public ReturnRequestEntity() {
    }

    public ReturnRequestEntity(String returnNumber, UUID orderId, String orderNumber, UUID orderItemId,
                               UUID customerId, String sku, String productName, int quantity,
                               BigDecimal originalUnitPrice, ReturnReason reason, ReturnResolutionType resolutionType,
                               List<String> photoUrls, String customerNotes, boolean codRefund) {
        this.returnNumber = returnNumber;
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.orderItemId = orderItemId;
        this.customerId = customerId;
        this.sku = sku;
        this.productName = productName;
        this.quantity = quantity;
        this.originalUnitPrice = originalUnitPrice;
        this.reason = reason;
        this.resolutionType = resolutionType != null ? resolutionType : ReturnResolutionType.EXCHANGE;
        this.status = ReturnRequestStatus.REQUESTED;
        this.photoUrls = photoUrls != null ? new ArrayList<>(photoUrls) : new ArrayList<>();
        this.customerNotes = customerNotes;
        this.codRefund = codRefund;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getReturnNumber() {
        return returnNumber;
    }

    public void setReturnNumber(String returnNumber) {
        this.returnNumber = returnNumber;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public UUID getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(UUID orderItemId) {
        this.orderItemId = orderItemId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getOriginalUnitPrice() {
        return originalUnitPrice;
    }

    public void setOriginalUnitPrice(BigDecimal originalUnitPrice) {
        this.originalUnitPrice = originalUnitPrice;
    }

    public ReturnReason getReason() {
        return reason;
    }

    public void setReason(ReturnReason reason) {
        this.reason = reason;
    }

    public ReturnResolutionType getResolutionType() {
        return resolutionType;
    }

    public void setResolutionType(ReturnResolutionType resolutionType) {
        this.resolutionType = resolutionType;
    }

    public ReturnRequestStatus getStatus() {
        return status;
    }

    public void setStatus(ReturnRequestStatus status) {
        this.status = status;
    }

    public List<String> getPhotoUrls() {
        return photoUrls;
    }

    public void setPhotoUrls(List<String> photoUrls) {
        this.photoUrls = photoUrls != null ? new ArrayList<>(photoUrls) : new ArrayList<>();
    }

    public String getCustomerNotes() {
        return customerNotes;
    }

    public void setCustomerNotes(String customerNotes) {
        this.customerNotes = customerNotes;
    }

    public TransportAgencyEntity getTransportAgency() {
        return transportAgency;
    }

    public void setTransportAgency(TransportAgencyEntity transportAgency) {
        this.transportAgency = transportAgency;
    }

    public String getPickupReference() {
        return pickupReference;
    }

    public void setPickupReference(String pickupReference) {
        this.pickupReference = pickupReference;
    }

    public LocalDate getPickupScheduledDate() {
        return pickupScheduledDate;
    }

    public void setPickupScheduledDate(LocalDate pickupScheduledDate) {
        this.pickupScheduledDate = pickupScheduledDate;
    }

    public Instant getPickedUpAt() {
        return pickedUpAt;
    }

    public void setPickedUpAt(Instant pickedUpAt) {
        this.pickedUpAt = pickedUpAt;
    }

    public Instant getReceivedAtWarehouseAt() {
        return receivedAtWarehouseAt;
    }

    public void setReceivedAtWarehouseAt(Instant receivedAtWarehouseAt) {
        this.receivedAtWarehouseAt = receivedAtWarehouseAt;
    }

    public UUID getReplacementProductId() {
        return replacementProductId;
    }

    public void setReplacementProductId(UUID replacementProductId) {
        this.replacementProductId = replacementProductId;
    }

    public UUID getReplacementVariantId() {
        return replacementVariantId;
    }

    public void setReplacementVariantId(UUID replacementVariantId) {
        this.replacementVariantId = replacementVariantId;
    }

    public String getReplacementSku() {
        return replacementSku;
    }

    public void setReplacementSku(String replacementSku) {
        this.replacementSku = replacementSku;
    }

    public String getReplacementProductName() {
        return replacementProductName;
    }

    public void setReplacementProductName(String replacementProductName) {
        this.replacementProductName = replacementProductName;
    }

    public BigDecimal getReplacementUnitPrice() {
        return replacementUnitPrice;
    }

    public void setReplacementUnitPrice(BigDecimal replacementUnitPrice) {
        this.replacementUnitPrice = replacementUnitPrice;
    }

    public BigDecimal getPriceDifference() {
        return priceDifference;
    }

    public void setPriceDifference(BigDecimal priceDifference) {
        this.priceDifference = priceDifference;
    }

    public boolean isPriceDifferenceSettled() {
        return priceDifferenceSettled;
    }

    public void setPriceDifferenceSettled(boolean priceDifferenceSettled) {
        this.priceDifferenceSettled = priceDifferenceSettled;
    }

    public ShipmentEntity getReplacementShipment() {
        return replacementShipment;
    }

    public void setReplacementShipment(ShipmentEntity replacementShipment) {
        this.replacementShipment = replacementShipment;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }

    public Instant getRefundDeadline() {
        return refundDeadline;
    }

    public void setRefundDeadline(Instant refundDeadline) {
        this.refundDeadline = refundDeadline;
    }

    public UUID getRefundApprovedBy() {
        return refundApprovedBy;
    }

    public void setRefundApprovedBy(UUID refundApprovedBy) {
        this.refundApprovedBy = refundApprovedBy;
    }

    public Instant getRefundApprovedAt() {
        return refundApprovedAt;
    }

    public void setRefundApprovedAt(Instant refundApprovedAt) {
        this.refundApprovedAt = refundApprovedAt;
    }

    public String getRefundRejectionReason() {
        return refundRejectionReason;
    }

    public void setRefundRejectionReason(String refundRejectionReason) {
        this.refundRejectionReason = refundRejectionReason;
    }

    public boolean isCodRefund() {
        return codRefund;
    }

    public void setCodRefund(boolean codRefund) {
        this.codRefund = codRefund;
    }

    public ReturnInspectionEntity getInspection() {
        return inspection;
    }

    public void setInspection(ReturnInspectionEntity inspection) {
        this.inspection = inspection;
        if (inspection != null) {
            inspection.setReturnRequest(this);
        }
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
