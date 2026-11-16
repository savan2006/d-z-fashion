package com.dzfashion.commerce.service;

import com.dzfashion.commerce.dto.request.AddTicketMessageRequest;
import com.dzfashion.commerce.dto.request.AssignTicketRequest;
import com.dzfashion.commerce.dto.request.CreateSupportTicketRequest;
import com.dzfashion.commerce.dto.request.EscalateTicketRequest;
import com.dzfashion.commerce.dto.request.ManualSupportIntakeRequest;
import com.dzfashion.commerce.dto.request.UpdateTicketStatusRequest;
import com.dzfashion.commerce.dto.response.PageResponse;
import com.dzfashion.commerce.dto.response.SupportTicketMessageResponse;
import com.dzfashion.commerce.dto.response.SupportTicketResponse;
import com.dzfashion.commerce.entity.SupportTicketEntity;
import com.dzfashion.commerce.entity.SupportTicketMessageEntity;
import com.dzfashion.commerce.enums.SupportTicketChannel;
import com.dzfashion.commerce.enums.SupportTicketPriority;
import com.dzfashion.commerce.enums.SupportTicketStatus;
import com.dzfashion.commerce.exception.InvalidOperationException;
import com.dzfashion.commerce.exception.ResourceNotFoundException;
import com.dzfashion.commerce.repository.SupportTicketMessageRepository;
import com.dzfashion.commerce.repository.SupportTicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SupportTicketService {

    private static final Logger log = LoggerFactory.getLogger(SupportTicketService.class);
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata");

    private final SupportTicketRepository ticketRepository;
    private final SupportTicketMessageRepository messageRepository;

    public SupportTicketService(SupportTicketRepository ticketRepository,
                                SupportTicketMessageRepository messageRepository) {
        this.ticketRepository = ticketRepository;
        this.messageRepository = messageRepository;
    }

    public SupportTicketResponse createTicket(UUID customerId, CreateSupportTicketRequest request) {
        String ticketNumber = generateTicketNumber();
        SupportTicketEntity ticket = new SupportTicketEntity(
                ticketNumber,
                customerId,
                request.orderId(),
                request.subject().trim(),
                SupportTicketChannel.WEB,
                request.priority() != null ? request.priority() : SupportTicketPriority.MEDIUM
        );

        SupportTicketMessageEntity initialMessage = new SupportTicketMessageEntity(
                customerId,
                "CUSTOMER",
                request.message().trim(),
                false
        );
        ticket.addMessage(initialMessage);

        SupportTicketEntity saved = ticketRepository.save(ticket);
        log.info("Created support ticket {} for customer {}", saved.getTicketNumber(), customerId);

        return mapToResponse(saved, false);
    }

    @Transactional(readOnly = true)
    public PageResponse<SupportTicketResponse> getTicketsForCustomer(UUID customerId, Pageable pageable) {
        Page<SupportTicketEntity> page = ticketRepository.findByCustomerId(customerId, pageable);
        List<SupportTicketResponse> content = page.getContent().stream()
                .map(t -> mapToResponse(t, false))
                .toList();

        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    @Transactional(readOnly = true)
    public SupportTicketResponse getCustomerTicketById(UUID customerId, UUID ticketId) {
        SupportTicketEntity ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Support ticket not found with ID: " + ticketId));

        if (!ticket.getCustomerId().equals(customerId)) {
            throw new ResourceNotFoundException("Support ticket not found with ID: " + ticketId);
        }

        return mapToResponse(ticket, false);
    }

    public SupportTicketResponse addCustomerMessage(UUID customerId, UUID ticketId, AddTicketMessageRequest request) {
        SupportTicketEntity ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Support ticket not found with ID: " + ticketId));

        if (!ticket.getCustomerId().equals(customerId)) {
            throw new ResourceNotFoundException("Support ticket not found with ID: " + ticketId);
        }

        if (ticket.getStatus() == SupportTicketStatus.CLOSED) {
            throw new InvalidOperationException("Cannot add message to a CLOSED support ticket: " + ticket.getTicketNumber());
        }

        SupportTicketMessageEntity msg = new SupportTicketMessageEntity(
                customerId,
                "CUSTOMER",
                request.message().trim(),
                false // Customer can never post internal notes
        );
        ticket.addMessage(msg);

        // If ticket was waiting on customer or resolved, reopen it to IN_PROGRESS
        if (ticket.getStatus() == SupportTicketStatus.WAITING_ON_CUSTOMER || ticket.getStatus() == SupportTicketStatus.RESOLVED) {
            ticket.setStatus(SupportTicketStatus.IN_PROGRESS);
        }
        ticket.setUpdatedAt(Instant.now());

        SupportTicketEntity saved = ticketRepository.save(ticket);
        log.info("Customer {} added message to ticket {}", customerId, saved.getTicketNumber());

        return mapToResponse(saved, false);
    }

    @Transactional(readOnly = true)
    public PageResponse<SupportTicketResponse> getAllTicketsForStaff(SupportTicketStatus status,
                                                                    SupportTicketChannel channel,
                                                                    Boolean escalated,
                                                                    Pageable pageable) {
        Specification<SupportTicketEntity> spec = (root, query, cb) -> cb.conjunction();
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (channel != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("channel"), channel));
        }
        if (escalated != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("escalated"), escalated));
        }

        Page<SupportTicketEntity> page = ticketRepository.findAll(spec, pageable);
        List<SupportTicketResponse> content = page.getContent().stream()
                .map(t -> mapToResponse(t, true))
                .toList();

        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    @Transactional(readOnly = true)
    public SupportTicketResponse getStaffTicketById(UUID ticketId) {
        SupportTicketEntity ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Support ticket not found with ID: " + ticketId));

        return mapToResponse(ticket, true);
    }

    public SupportTicketResponse assignTicket(UUID ticketId, AssignTicketRequest request, UUID staffId) {
        SupportTicketEntity ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Support ticket not found with ID: " + ticketId));

        ticket.setAssignedManagerId(request.managerId());
        if (ticket.getStatus() == SupportTicketStatus.OPEN) {
            ticket.setStatus(SupportTicketStatus.IN_PROGRESS);
        }
        ticket.setUpdatedAt(Instant.now());

        SupportTicketEntity saved = ticketRepository.save(ticket);
        log.info("Ticket {} assigned to manager {} by staff {}", saved.getTicketNumber(), request.managerId(), staffId);

        return mapToResponse(saved, true);
    }

    public SupportTicketResponse updateTicketStatus(UUID ticketId, UpdateTicketStatusRequest request, UUID staffId, String staffRole) {
        SupportTicketEntity ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Support ticket not found with ID: " + ticketId));

        ticket.setStatus(request.status());
        Instant now = Instant.now();
        if (request.status() == SupportTicketStatus.RESOLVED || request.status() == SupportTicketStatus.CLOSED) {
            ticket.setResolvedAt(now);
        }
        ticket.setUpdatedAt(now);

        if (request.note() != null && !request.note().isBlank()) {
            SupportTicketMessageEntity statusNote = new SupportTicketMessageEntity(
                    staffId,
                    staffRole,
                    "Status changed to " + request.status() + ": " + request.note().trim(),
                    true // Recorded as internal audit note
            );
            ticket.addMessage(statusNote);
        }

        SupportTicketEntity saved = ticketRepository.save(ticket);
        log.info("Ticket {} status updated to {} by {} {}", saved.getTicketNumber(), request.status(), staffRole, staffId);

        return mapToResponse(saved, true);
    }

    public SupportTicketResponse addStaffMessage(UUID ticketId, AddTicketMessageRequest request, UUID staffId, String staffRole) {
        SupportTicketEntity ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Support ticket not found with ID: " + ticketId));

        SupportTicketMessageEntity msg = new SupportTicketMessageEntity(
                staffId,
                staffRole,
                request.message().trim(),
                request.internalNote()
        );
        ticket.addMessage(msg);

        // If staff sent a public message, update status to WAITING_ON_CUSTOMER unless closed
        if (!request.internalNote() && ticket.getStatus() != SupportTicketStatus.CLOSED) {
            ticket.setStatus(SupportTicketStatus.WAITING_ON_CUSTOMER);
        }
        ticket.setUpdatedAt(Instant.now());

        SupportTicketEntity saved = ticketRepository.save(ticket);
        log.info("Staff {} ({}) posted message to ticket {} (internal: {})",
                staffId, staffRole, saved.getTicketNumber(), request.internalNote());

        return mapToResponse(saved, true);
    }

    public SupportTicketResponse recordManualIntake(ManualSupportIntakeRequest request, UUID staffId, String staffRole) {
        String ticketNumber = generateTicketNumber();
        SupportTicketEntity ticket = new SupportTicketEntity(
                ticketNumber,
                request.customerId(),
                request.orderId(),
                request.subject().trim(),
                request.channel(),
                request.priority() != null ? request.priority() : SupportTicketPriority.MEDIUM
        );
        ticket.setAssignedManagerId(staffId);
        ticket.setStatus(SupportTicketStatus.IN_PROGRESS);

        SupportTicketMessageEntity intakeMsg = new SupportTicketMessageEntity(
                staffId,
                staffRole,
                "Manual intake via " + request.channel() + ":\n" + request.notes().trim(),
                false
        );
        ticket.addMessage(intakeMsg);

        SupportTicketEntity saved = ticketRepository.save(ticket);
        log.info("Staff {} recorded manual {} intake ticket {} for customer {}",
                staffId, request.channel(), saved.getTicketNumber(), request.customerId());

        return mapToResponse(saved, true);
    }

    public SupportTicketResponse escalateTicket(UUID ticketId, EscalateTicketRequest request, UUID ownerId) {
        SupportTicketEntity ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Support ticket not found with ID: " + ticketId));

        Instant now = Instant.now();
        ticket.setEscalated(true);
        ticket.setEscalationReason(request.escalationReason().trim());
        ticket.setEscalatedAt(now);
        ticket.setStatus(SupportTicketStatus.ESCALATED);
        ticket.setUpdatedAt(now);

        SupportTicketMessageEntity note = new SupportTicketMessageEntity(
                ownerId,
                "OWNER",
                "Escalated by Owner: " + request.escalationReason().trim(),
                true
        );
        ticket.addMessage(note);

        SupportTicketEntity saved = ticketRepository.save(ticket);
        log.info("Support ticket {} escalated by Owner {}. Reason: {}", saved.getTicketNumber(), ownerId, request.escalationReason());

        return mapToResponse(saved, true);
    }

    private SupportTicketResponse mapToResponse(SupportTicketEntity entity, boolean isStaffView) {
        List<SupportTicketMessageResponse> messageList;
        if (isStaffView) {
            messageList = entity.getMessages().stream()
                    .map(this::mapMessageToResponse)
                    .toList();
        } else {
            messageList = entity.getMessages().stream()
                    .filter(m -> !m.isInternalNote()) // Customer cannot see internal notes!
                    .map(this::mapMessageToResponse)
                    .toList();
        }

        return new SupportTicketResponse(
                entity.getId(),
                entity.getTicketNumber(),
                entity.getCustomerId(),
                entity.getOrderId(),
                entity.getSubject(),
                entity.getChannel(),
                entity.getPriority(),
                entity.getStatus(),
                entity.getAssignedManagerId(),
                entity.isEscalated(),
                entity.getEscalationReason(),
                entity.getEscalatedAt(),
                entity.getResolvedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                messageList
        );
    }

    private SupportTicketMessageResponse mapMessageToResponse(SupportTicketMessageEntity msg) {
        return new SupportTicketMessageResponse(
                msg.getId(),
                msg.getTicket() != null ? msg.getTicket().getId() : null,
                msg.getSenderId(),
                msg.getSenderRole(),
                msg.getMessage(),
                msg.isInternalNote(),
                msg.getCreatedAt()
        );
    }

    private String generateTicketNumber() {
        String dateStr = LocalDate.now(BUSINESS_ZONE).format(DateTimeFormatter.BASIC_ISO_DATE);
        String randomSuffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "TCK-" + dateStr + "-" + randomSuffix;
    }
}
