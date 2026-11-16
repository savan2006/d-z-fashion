package com.dzfashion.fulfillment.entity;

import com.dzfashion.fulfillment.enums.ItemDisposition;
import com.dzfashion.fulfillment.enums.ReturnInspectionResult;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "return_inspections")
public class ReturnInspectionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "return_request_id", nullable = false, unique = true)
    private ReturnRequestEntity returnRequest;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "inspected_by", nullable = false)
    private UUID inspectedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 20)
    private ReturnInspectionResult result;

    @Enumerated(EnumType.STRING)
    @Column(name = "disposition", nullable = false, length = 30)
    private ItemDisposition disposition = ItemDisposition.DAMAGED_QUARANTINE;

    @Column(name = "condition_notes", nullable = false, columnDefinition = "TEXT")
    private String conditionNotes;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "photo_urls", columnDefinition = "text[]")
    private List<String> photoUrls = new ArrayList<>();

    @Column(name = "inspected_at", nullable = false)
    private Instant inspectedAt = Instant.now();

    public ReturnInspectionEntity() {
    }

    public ReturnInspectionEntity(ReturnRequestEntity returnRequest, UUID orderId, UUID inspectedBy,
                                  ReturnInspectionResult result, ItemDisposition disposition,
                                  String conditionNotes, List<String> photoUrls) {
        this.returnRequest = returnRequest;
        this.orderId = orderId;
        this.inspectedBy = inspectedBy;
        this.result = result;
        this.disposition = disposition != null ? disposition : ItemDisposition.DAMAGED_QUARANTINE;
        this.conditionNotes = conditionNotes;
        this.photoUrls = photoUrls != null ? new ArrayList<>(photoUrls) : new ArrayList<>();
        this.inspectedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ReturnRequestEntity getReturnRequest() {
        return returnRequest;
    }

    public void setReturnRequest(ReturnRequestEntity returnRequest) {
        this.returnRequest = returnRequest;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public UUID getInspectedBy() {
        return inspectedBy;
    }

    public void setInspectedBy(UUID inspectedBy) {
        this.inspectedBy = inspectedBy;
    }

    public ReturnInspectionResult getResult() {
        return result;
    }

    public void setResult(ReturnInspectionResult result) {
        this.result = result;
    }

    public ItemDisposition getDisposition() {
        return disposition;
    }

    public void setDisposition(ItemDisposition disposition) {
        this.disposition = disposition;
    }

    public String getConditionNotes() {
        return conditionNotes;
    }

    public void setConditionNotes(String conditionNotes) {
        this.conditionNotes = conditionNotes;
    }

    public List<String> getPhotoUrls() {
        return photoUrls;
    }

    public void setPhotoUrls(List<String> photoUrls) {
        this.photoUrls = photoUrls != null ? new ArrayList<>(photoUrls) : new ArrayList<>();
    }

    public Instant getInspectedAt() {
        return inspectedAt;
    }

    public void setInspectedAt(Instant inspectedAt) {
        this.inspectedAt = inspectedAt;
    }
}
