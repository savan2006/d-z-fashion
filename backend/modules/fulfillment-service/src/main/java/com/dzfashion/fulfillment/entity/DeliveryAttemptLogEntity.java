package com.dzfashion.fulfillment.entity;

import com.dzfashion.fulfillment.enums.DeliveryAttemptResult;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "delivery_attempt_logs")
public class DeliveryAttemptLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private ShipmentEntity shipment;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 50)
    private DeliveryAttemptResult result;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "recorded_by", nullable = false)
    private UUID recordedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public DeliveryAttemptLogEntity() {
    }

    public DeliveryAttemptLogEntity(int attemptNumber, Instant attemptedAt, DeliveryAttemptResult result, String notes, UUID recordedBy) {
        this.attemptNumber = attemptNumber;
        this.attemptedAt = attemptedAt != null ? attemptedAt : Instant.now();
        this.result = result;
        this.notes = notes;
        this.recordedBy = recordedBy;
        this.createdAt = Instant.now();
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

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(int attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public Instant getAttemptedAt() {
        return attemptedAt;
    }

    public void setAttemptedAt(Instant attemptedAt) {
        this.attemptedAt = attemptedAt;
    }

    public DeliveryAttemptResult getResult() {
        return result;
    }

    public void setResult(DeliveryAttemptResult result) {
        this.result = result;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public UUID getRecordedBy() {
        return recordedBy;
    }

    public void setRecordedBy(UUID recordedBy) {
        this.recordedBy = recordedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
