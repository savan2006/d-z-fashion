package com.dzfashion.commerce.entity;

import com.dzfashion.commerce.enums.SupportTicketChannel;
import com.dzfashion.commerce.enums.SupportTicketPriority;
import com.dzfashion.commerce.enums.SupportTicketStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "support_tickets")
public class SupportTicketEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "ticket_number", nullable = false, unique = true, length = 32)
    private String ticketNumber;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "subject", nullable = false, length = 200)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private SupportTicketChannel channel = SupportTicketChannel.WEB;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private SupportTicketPriority priority = SupportTicketPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SupportTicketStatus status = SupportTicketStatus.OPEN;

    @Column(name = "assigned_manager_id")
    private UUID assignedManagerId;

    @Column(name = "is_escalated", nullable = false)
    private boolean escalated = false;

    @Column(name = "escalation_reason", columnDefinition = "TEXT")
    private String escalationReason;

    @Column(name = "escalated_at")
    private Instant escalatedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<SupportTicketMessageEntity> messages = new ArrayList<>();

    public SupportTicketEntity() {
    }

    public SupportTicketEntity(String ticketNumber, UUID customerId, UUID orderId, String subject,
                               SupportTicketChannel channel, SupportTicketPriority priority) {
        this.ticketNumber = ticketNumber;
        this.customerId = customerId;
        this.orderId = orderId;
        this.subject = subject;
        this.channel = channel != null ? channel : SupportTicketChannel.WEB;
        this.priority = priority != null ? priority : SupportTicketPriority.MEDIUM;
        this.status = SupportTicketStatus.OPEN;
        this.escalated = false;
    }

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public void addMessage(SupportTicketMessageEntity message) {
        messages.add(message);
        message.setTicket(this);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public void setTicketNumber(String ticketNumber) {
        this.ticketNumber = ticketNumber;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public SupportTicketChannel getChannel() {
        return channel;
    }

    public void setChannel(SupportTicketChannel channel) {
        this.channel = channel;
    }

    public SupportTicketPriority getPriority() {
        return priority;
    }

    public void setPriority(SupportTicketPriority priority) {
        this.priority = priority;
    }

    public SupportTicketStatus getStatus() {
        return status;
    }

    public void setStatus(SupportTicketStatus status) {
        this.status = status;
    }

    public UUID getAssignedManagerId() {
        return assignedManagerId;
    }

    public void setAssignedManagerId(UUID assignedManagerId) {
        this.assignedManagerId = assignedManagerId;
    }

    public boolean isEscalated() {
        return escalated;
    }

    public void setEscalated(boolean escalated) {
        this.escalated = escalated;
    }

    public String getEscalationReason() {
        return escalationReason;
    }

    public void setEscalationReason(String escalationReason) {
        this.escalationReason = escalationReason;
    }

    public Instant getEscalatedAt() {
        return escalatedAt;
    }

    public void setEscalatedAt(Instant escalatedAt) {
        this.escalatedAt = escalatedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
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

    public List<SupportTicketMessageEntity> getMessages() {
        return messages;
    }

    public void setMessages(List<SupportTicketMessageEntity> messages) {
        this.messages = messages;
    }
}
