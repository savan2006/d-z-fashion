package com.dzfashion.fulfillment.entity;

import com.dzfashion.fulfillment.enums.AgencyCodSettlementStatus;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "agency_cod_settlements")
public class AgencyCodSettlementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "batch_reference", nullable = false, unique = true, length = 64)
    private String batchReference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transport_agency_id", nullable = false)
    private TransportAgencyEntity transportAgency;

    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;

    @Column(name = "total_delivered_orders", nullable = false)
    private int totalDeliveredOrders = 0;

    @Column(name = "total_expected_cod", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalExpectedCod = BigDecimal.ZERO;

    @Column(name = "remitted_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal remittedAmount = BigDecimal.ZERO;

    @Column(name = "discrepancy_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discrepancyAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AgencyCodSettlementStatus status = AgencyCodSettlementStatus.PREPARED;

    @Column(name = "prepared_by", nullable = false)
    private UUID preparedBy;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "proof_reference", length = 100)
    private String proofReference;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "approved_at")
    private Instant approvedAt;

    public AgencyCodSettlementEntity() {
    }

    public AgencyCodSettlementEntity(String batchReference, TransportAgencyEntity transportAgency, LocalDate settlementDate, int totalDeliveredOrders, BigDecimal totalExpectedCod, BigDecimal remittedAmount, BigDecimal discrepancyAmount, UUID preparedBy, String proofReference, String notes) {
        this.batchReference = batchReference;
        this.transportAgency = transportAgency;
        this.settlementDate = settlementDate;
        this.totalDeliveredOrders = totalDeliveredOrders;
        this.totalExpectedCod = totalExpectedCod;
        this.remittedAmount = remittedAmount;
        this.discrepancyAmount = discrepancyAmount;
        this.preparedBy = preparedBy;
        this.proofReference = proofReference;
        this.notes = notes;
        this.status = AgencyCodSettlementStatus.PREPARED;
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

    public TransportAgencyEntity getTransportAgency() {
        return transportAgency;
    }

    public void setTransportAgency(TransportAgencyEntity transportAgency) {
        this.transportAgency = transportAgency;
    }

    public LocalDate getSettlementDate() {
        return settlementDate;
    }

    public void setSettlementDate(LocalDate settlementDate) {
        this.settlementDate = settlementDate;
    }

    public int getTotalDeliveredOrders() {
        return totalDeliveredOrders;
    }

    public void setTotalDeliveredOrders(int totalDeliveredOrders) {
        this.totalDeliveredOrders = totalDeliveredOrders;
    }

    public BigDecimal getTotalExpectedCod() {
        return totalExpectedCod;
    }

    public void setTotalExpectedCod(BigDecimal totalExpectedCod) {
        this.totalExpectedCod = totalExpectedCod;
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

    public AgencyCodSettlementStatus getStatus() {
        return status;
    }

    public void setStatus(AgencyCodSettlementStatus status) {
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

    public String getProofReference() {
        return proofReference;
    }

    public void setProofReference(String proofReference) {
        this.proofReference = proofReference;
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

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }
}
