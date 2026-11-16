package com.dzfashion.payment.entity;

import com.dzfashion.payment.enums.CodSettlementStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "cod_settlement_batches")
public class CodSettlementBatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "batch_reference", nullable = false, unique = true, length = 64)
    private String batchReference;

    @Column(name = "transport_agency_name", nullable = false, length = 100)
    private String transportAgencyName;

    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;

    @Column(name = "total_collected_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalCollectedAmount;

    @Column(name = "remitted_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal remittedAmount;

    @Column(name = "discrepancy_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discrepancyAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CodSettlementStatus status = CodSettlementStatus.SUBMITTED;

    @Column(name = "prepared_by", nullable = false)
    private UUID preparedBy;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "approved_at")
    private Instant approvedAt;

    public CodSettlementBatchEntity() {
    }

    public CodSettlementBatchEntity(String batchReference, String transportAgencyName, LocalDate settlementDate, BigDecimal totalCollectedAmount, BigDecimal remittedAmount, BigDecimal discrepancyAmount, UUID preparedBy, String notes) {
        this.batchReference = batchReference;
        this.transportAgencyName = transportAgencyName;
        this.settlementDate = settlementDate;
        this.totalCollectedAmount = totalCollectedAmount;
        this.remittedAmount = remittedAmount;
        this.discrepancyAmount = discrepancyAmount;
        this.status = CodSettlementStatus.SUBMITTED;
        this.preparedBy = preparedBy;
        this.notes = notes;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getBatchReference() {
        return batchReference;
    }

    public void setBatchReference(String batchReference) {
        this.batchReference = batchReference;
    }

    public String getTransportAgencyName() {
        return transportAgencyName;
    }

    public void setTransportAgencyName(String transportAgencyName) {
        this.transportAgencyName = transportAgencyName;
    }

    public LocalDate getSettlementDate() {
        return settlementDate;
    }

    public void setSettlementDate(LocalDate settlementDate) {
        this.settlementDate = settlementDate;
    }

    public BigDecimal getTotalCollectedAmount() {
        return totalCollectedAmount;
    }

    public void setTotalCollectedAmount(BigDecimal totalCollectedAmount) {
        this.totalCollectedAmount = totalCollectedAmount;
    }

    public BigDecimal getRemittedAmount() {
        return remittedAmount;
    }

    public void setRemittedAmount(BigDecimal remittedAmount) {
        this.remittedAmount = remittedAmount;
    }

    public BigDecimal getDiscrepancyAmount() {
        return discrepancyAmount;
    }

    public void setDiscrepancyAmount(BigDecimal discrepancyAmount) {
        this.discrepancyAmount = discrepancyAmount;
    }

    public CodSettlementStatus getStatus() {
        return status;
    }

    public void setStatus(CodSettlementStatus status) {
        this.status = status;
    }

    public UUID getPreparedBy() {
        return preparedBy;
    }

    public void setPreparedBy(UUID preparedBy) {
        this.preparedBy = preparedBy;
    }

    public UUID getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(UUID approvedBy) {
        this.approvedBy = approvedBy;
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

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }
}
