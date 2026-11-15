package com.dzfashion.commerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cod_serviceable_pincodes")
public class CodServiceablePincodeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "pincode", nullable = false, unique = true, length = 10)
    private String pincode;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "enabled_by", nullable = false)
    private UUID enabledBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public CodServiceablePincodeEntity() {
    }

    public CodServiceablePincodeEntity(String pincode, boolean active, UUID enabledBy) {
        this.pincode = pincode;
        this.active = active;
        this.enabledBy = enabledBy;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        this.updatedAt = Instant.now();
    }

    public UUID getEnabledBy() {
        return enabledBy;
    }

    public void setEnabledBy(UUID enabledBy) {
        this.enabledBy = enabledBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
