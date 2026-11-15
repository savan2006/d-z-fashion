package com.dzfashion.commerce.entity;

import com.dzfashion.commerce.enums.OrderClaimAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_claim_history")
public class OrderClaimHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 30)
    private OrderClaimAction action;

    @Column(name = "previous_manager_id")
    private UUID previousManagerId;

    @Column(name = "new_manager_id")
    private UUID newManagerId;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public OrderClaimHistoryEntity() {
    }

    public OrderClaimHistoryEntity(UUID orderId, UUID actorId, OrderClaimAction action, UUID previousManagerId, UUID newManagerId, String reason) {
        this.orderId = orderId;
        this.actorId = actorId;
        this.action = action;
        this.previousManagerId = previousManagerId;
        this.newManagerId = newManagerId;
        this.reason = reason;
        this.createdAt = Instant.now();
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

    public UUID getActorId() {
        return actorId;
    }

    public void setActorId(UUID actorId) {
        this.actorId = actorId;
    }

    public OrderClaimAction getAction() {
        return action;
    }

    public void setAction(OrderClaimAction action) {
        this.action = action;
    }

    public UUID getPreviousManagerId() {
        return previousManagerId;
    }

    public void setPreviousManagerId(UUID previousManagerId) {
        this.previousManagerId = previousManagerId;
    }

    public UUID getNewManagerId() {
        return newManagerId;
    }

    public void setNewManagerId(UUID newManagerId) {
        this.newManagerId = newManagerId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
