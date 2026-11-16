package com.dzfashion.fulfillment.entity;

import com.dzfashion.fulfillment.enums.DailyDispatchStatus;
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
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "daily_dispatch_batches")
public class DailyDispatchBatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "batch_number", nullable = false, unique = true, length = 64)
    private String batchNumber;

    @Column(name = "dispatch_date", nullable = false)
    private LocalDate dispatchDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transport_agency_id")
    private TransportAgencyEntity transportAgency;

    @Column(name = "prepared_by", nullable = false)
    private UUID preparedBy;

    @Column(name = "parcel_count", nullable = false)
    private int parcelCount = 0;

    @Column(name = "bill_reference", length = 100)
    private String billReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DailyDispatchStatus status = DailyDispatchStatus.PREPARED;

    @Column(name = "handover_at")
    private Instant handoverAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public DailyDispatchBatchEntity() {
    }

    public DailyDispatchBatchEntity(String batchNumber, LocalDate dispatchDate, TransportAgencyEntity transportAgency, UUID preparedBy, int parcelCount, String billReference, String notes) {
        this.batchNumber = batchNumber;
        this.dispatchDate = dispatchDate;
        this.transportAgency = transportAgency;
        this.preparedBy = preparedBy;
        this.parcelCount = parcelCount;
        this.billReference = billReference;
        this.status = DailyDispatchStatus.PREPARED;
        this.notes = notes;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public LocalDate getDispatchDate() {
        return dispatchDate;
    }

    public void setDispatchDate(LocalDate dispatchDate) {
        this.dispatchDate = dispatchDate;
    }

    public TransportAgencyEntity getTransportAgency() {
        return transportAgency;
    }

    public void setTransportAgency(TransportAgencyEntity transportAgency) {
        this.transportAgency = transportAgency;
    }

    public UUID getPreparedBy() {
        return preparedBy;
    }

    public void setPreparedBy(UUID preparedBy) {
        this.preparedBy = preparedBy;
    }

    public int getParcelCount() {
        return parcelCount;
    }

    public void setParcelCount(int parcelCount) {
        this.parcelCount = parcelCount;
    }

    public String getBillReference() {
        return billReference;
    }

    public void setBillReference(String billReference) {
        this.billReference = billReference;
    }

    public DailyDispatchStatus getStatus() {
        return status;
    }

    public void setStatus(DailyDispatchStatus status) {
        this.status = status;
    }

    public Instant getHandoverAt() {
        return handoverAt;
    }

    public void setHandoverAt(Instant handoverAt) {
        this.handoverAt = handoverAt;
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
}
