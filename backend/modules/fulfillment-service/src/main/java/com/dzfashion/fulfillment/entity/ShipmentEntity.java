package com.dzfashion.fulfillment.entity;

import com.dzfashion.fulfillment.enums.ShipmentStatus;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "shipments")
public class ShipmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(name = "order_number", length = 64)
    private String orderNumber;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "recipient_name", length = 100)
    private String recipientName;

    @Column(name = "recipient_phone", length = 20)
    private String recipientPhone;

    @Column(name = "shipping_address", columnDefinition = "TEXT")
    private String shippingAddress;

    @Column(name = "city", length = 50)
    private String city;

    @Column(name = "state", length = 50)
    private String state;

    @Column(name = "pincode", length = 10)
    private String pincode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transport_agency_id")
    private TransportAgencyEntity transportAgency;

    @Column(name = "tracking_reference", length = 100)
    private String trackingReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ShipmentStatus status = ShipmentStatus.PROCESSING;

    @Column(name = "dispatch_date")
    private LocalDate dispatchDate;

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    @Column(name = "delivery_attempts", nullable = false)
    private int deliveryAttempts = 0;

    @Column(name = "is_cod", nullable = false)
    private boolean cod = false;

    @Column(name = "cod_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal codAmount = BigDecimal.ZERO;

    @Column(name = "cod_collected_amount", precision = 12, scale = 2)
    private BigDecimal codCollectedAmount = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_dispatch_batch_id")
    private DailyDispatchBatchEntity dailyDispatchBatch;

    @Column(name = "picked_at")
    private Instant pickedAt;

    @Column(name = "packed_at")
    private Instant packedAt;

    @Column(name = "dispatched_at")
    private Instant dispatchedAt;

    @Column(name = "returned_to_warehouse_at")
    private Instant returnedToWarehouseAt;

    @Column(name = "eligible_return_until")
    private Instant eligibleReturnUntil;

    @Column(name = "cod_settlement_batch_id")
    private UUID codSettlementBatchId;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShipmentItemEntity> items = new ArrayList<>();

    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DeliveryAttemptLogEntity> attemptLogs = new ArrayList<>();

    public ShipmentEntity() {
    }

    public ShipmentEntity(UUID orderId, String orderNumber, UUID customerId, String recipientName, String recipientPhone, String shippingAddress, String city, String state, String pincode, boolean cod, BigDecimal codAmount) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.customerId = customerId;
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.shippingAddress = shippingAddress;
        this.city = city;
        this.state = state;
        this.pincode = pincode;
        this.cod = cod;
        this.codAmount = codAmount != null ? codAmount : BigDecimal.ZERO;
        this.status = ShipmentStatus.PROCESSING;
        this.deliveryAttempts = 0;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getRecipientPhone() {
        return recipientPhone;
    }

    public void setRecipientPhone(String recipientPhone) {
        this.recipientPhone = recipientPhone;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public TransportAgencyEntity getTransportAgency() {
        return transportAgency;
    }

    public void setTransportAgency(TransportAgencyEntity transportAgency) {
        this.transportAgency = transportAgency;
    }

    public String getTrackingReference() {
        return trackingReference;
    }

    public void setTrackingReference(String trackingReference) {
        this.trackingReference = trackingReference;
    }

    public ShipmentStatus getStatus() {
        return status;
    }

    public void setStatus(ShipmentStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public LocalDate getDispatchDate() {
        return dispatchDate;
    }

    public void setDispatchDate(LocalDate dispatchDate) {
        this.dispatchDate = dispatchDate;
    }

    public LocalDate getDeliveryDate() {
        return deliveryDate;
    }

    public void setDeliveryDate(LocalDate deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    public int getDeliveryAttempts() {
        return deliveryAttempts;
    }

    public void setDeliveryAttempts(int deliveryAttempts) {
        this.deliveryAttempts = deliveryAttempts;
    }

    public boolean isCod() {
        return cod;
    }

    public void setCod(boolean cod) {
        this.cod = cod;
    }

    public BigDecimal getCodAmount() {
        return codAmount;
    }

    public void setCodAmount(BigDecimal codAmount) {
        this.codAmount = codAmount;
    }

    public BigDecimal getCodCollectedAmount() {
        return codCollectedAmount;
    }

    public void setCodCollectedAmount(BigDecimal codCollectedAmount) {
        this.codCollectedAmount = codCollectedAmount;
    }

    public DailyDispatchBatchEntity getDailyDispatchBatch() {
        return dailyDispatchBatch;
    }

    public void setDailyDispatchBatch(DailyDispatchBatchEntity dailyDispatchBatch) {
        this.dailyDispatchBatch = dailyDispatchBatch;
    }

    public Instant getPickedAt() {
        return pickedAt;
    }

    public void setPickedAt(Instant pickedAt) {
        this.pickedAt = pickedAt;
    }

    public Instant getPackedAt() {
        return packedAt;
    }

    public void setPackedAt(Instant packedAt) {
        this.packedAt = packedAt;
    }

    public Instant getDispatchedAt() {
        return dispatchedAt;
    }

    public void setDispatchedAt(Instant dispatchedAt) {
        this.dispatchedAt = dispatchedAt;
    }

    public Instant getReturnedToWarehouseAt() {
        return returnedToWarehouseAt;
    }

    public void setReturnedToWarehouseAt(Instant returnedToWarehouseAt) {
        this.returnedToWarehouseAt = returnedToWarehouseAt;
    }

    public Instant getEligibleReturnUntil() {
        return eligibleReturnUntil;
    }

    public void setEligibleReturnUntil(Instant eligibleReturnUntil) {
        this.eligibleReturnUntil = eligibleReturnUntil;
    }

    public UUID getCodSettlementBatchId() {
        return codSettlementBatchId;
    }

    public void setCodSettlementBatchId(UUID codSettlementBatchId) {
        this.codSettlementBatchId = codSettlementBatchId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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

    public List<ShipmentItemEntity> getItems() {
        return items;
    }

    public void setItems(List<ShipmentItemEntity> items) {
        this.items = items;
    }

    public List<DeliveryAttemptLogEntity> getAttemptLogs() {
        return attemptLogs;
    }

    public void setAttemptLogs(List<DeliveryAttemptLogEntity> attemptLogs) {
        this.attemptLogs = attemptLogs;
    }

    public void addItem(ShipmentItemEntity item) {
        items.add(item);
        item.setShipment(this);
    }

    public void addAttemptLog(DeliveryAttemptLogEntity log) {
        attemptLogs.add(log);
        log.setShipment(this);
    }
}
