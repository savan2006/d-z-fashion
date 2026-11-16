package com.dzfashion.commerce.service;

import com.dzfashion.commerce.dto.request.AddTicketMessageRequest;
import com.dzfashion.commerce.dto.request.AssignTicketRequest;
import com.dzfashion.commerce.dto.request.CreateSupportTicketRequest;
import com.dzfashion.commerce.dto.request.EscalateTicketRequest;
import com.dzfashion.commerce.dto.request.ManualSupportIntakeRequest;
import com.dzfashion.commerce.dto.request.UpdateTicketStatusRequest;
import com.dzfashion.commerce.dto.response.SupportTicketResponse;
import com.dzfashion.commerce.entity.SupportTicketEntity;
import com.dzfashion.commerce.entity.SupportTicketMessageEntity;
import com.dzfashion.commerce.enums.SupportTicketChannel;
import com.dzfashion.commerce.enums.SupportTicketPriority;
import com.dzfashion.commerce.enums.SupportTicketStatus;
import com.dzfashion.commerce.repository.SupportTicketMessageRepository;
import com.dzfashion.commerce.repository.SupportTicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupportTicketServiceTest {

    @Mock
    private SupportTicketRepository ticketRepository;

    @Mock
    private SupportTicketMessageRepository messageRepository;

    @InjectMocks
    private SupportTicketService supportTicketService;

    private UUID customerId;
    private UUID ticketId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        ticketId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Customer successfully creates a support ticket with initial message")
    void testCreateTicket_CustomerSuccess() {
        CreateSupportTicketRequest request = new CreateSupportTicketRequest(
                UUID.randomUUID(), "Order delivery delay", "Where is my parcel?", SupportTicketPriority.HIGH
        );

        when(ticketRepository.save(any(SupportTicketEntity.class))).thenAnswer(invocation -> {
            SupportTicketEntity entity = invocation.getArgument(0);
            entity.setId(ticketId);
            return entity;
        });

        SupportTicketResponse response = supportTicketService.createTicket(customerId, request);

        assertThat(response).isNotNull();
        assertThat(response.ticketNumber()).startsWith("TCK-");
        assertThat(response.subject()).isEqualTo("Order delivery delay");
        assertThat(response.channel()).isEqualTo(SupportTicketChannel.WEB);
        assertThat(response.priority()).isEqualTo(SupportTicketPriority.HIGH);
        assertThat(response.status()).isEqualTo(SupportTicketStatus.OPEN);
        assertThat(response.messages()).hasSize(1);
        assertThat(response.messages().get(0).message()).isEqualTo("Where is my parcel?");
        verify(ticketRepository).save(any(SupportTicketEntity.class));
    }

    @Test
    @DisplayName("Customer view strictly excludes internal staff notes from message list")
    void testCustomerCannotSeeInternalNotes() {
        SupportTicketEntity ticket = new SupportTicketEntity(
                "TCK-20261116-TEST01", customerId, null, "Fabric question", SupportTicketChannel.WEB, SupportTicketPriority.LOW
        );
        ticket.setId(ticketId);

        // 1 public message and 1 internal note
        SupportTicketMessageEntity publicMsg = new SupportTicketMessageEntity(customerId, "CUSTOMER", "Public inquiry", false);
        SupportTicketMessageEntity internalNote = new SupportTicketMessageEntity(UUID.randomUUID(), "MANAGER", "Internal staff note: check warehouse", true);
        ticket.addMessage(publicMsg);
        ticket.addMessage(internalNote);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        SupportTicketResponse response = supportTicketService.getCustomerTicketById(customerId, ticketId);

        assertThat(response.messages()).hasSize(1);
        assertThat(response.messages().get(0).message()).isEqualTo("Public inquiry");
        assertThat(response.messages().get(0).internalNote()).isFalse();
    }

    @Test
    @DisplayName("Staff view includes all messages including internal notes")
    void testStaffCanSeeAllNotes() {
        SupportTicketEntity ticket = new SupportTicketEntity(
                "TCK-20261116-TEST01", customerId, null, "Fabric question", SupportTicketChannel.WEB, SupportTicketPriority.LOW
        );
        ticket.setId(ticketId);

        SupportTicketMessageEntity publicMsg = new SupportTicketMessageEntity(customerId, "CUSTOMER", "Public inquiry", false);
        SupportTicketMessageEntity internalNote = new SupportTicketMessageEntity(UUID.randomUUID(), "MANAGER", "Internal staff note: check warehouse", true);
        ticket.addMessage(publicMsg);
        ticket.addMessage(internalNote);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        SupportTicketResponse response = supportTicketService.getStaffTicketById(ticketId);

        assertThat(response.messages()).hasSize(2);
    }

    @Test
    @DisplayName("Staff records manual inquiry intake from WhatsApp or phone")
    void testRecordManualIntake_WhatsApp() {
        UUID staffId = UUID.randomUUID();
        ManualSupportIntakeRequest request = new ManualSupportIntakeRequest(
                customerId, null, SupportTicketChannel.WHATSAPP, "Urgent size exchange query",
                "Customer messaged on WhatsApp asking if size L is in stock", SupportTicketPriority.HIGH
        );

        when(ticketRepository.save(any(SupportTicketEntity.class))).thenAnswer(invocation -> {
            SupportTicketEntity entity = invocation.getArgument(0);
            entity.setId(ticketId);
            return entity;
        });

        SupportTicketResponse response = supportTicketService.recordManualIntake(request, staffId, "MANAGER");

        assertThat(response.channel()).isEqualTo(SupportTicketChannel.WHATSAPP);
        assertThat(response.status()).isEqualTo(SupportTicketStatus.IN_PROGRESS);
        assertThat(response.assignedManagerId()).isEqualTo(staffId);
        assertThat(response.messages()).hasSize(1);
        assertThat(response.messages().get(0).message()).contains("WhatsApp");
    }

    @Test
    @DisplayName("Owner escalates a support ticket with escalation reason")
    void testEscalateTicket_OwnerSuccess() {
        UUID ownerId = UUID.randomUUID();
        SupportTicketEntity ticket = new SupportTicketEntity(
                "TCK-20261116-TEST01", customerId, null, "Severe delay", SupportTicketChannel.WEB, SupportTicketPriority.URGENT
        );
        ticket.setId(ticketId);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(SupportTicketEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EscalateTicketRequest request = new EscalateTicketRequest("Parcel delayed over 8 days without courier update");
        SupportTicketResponse response = supportTicketService.escalateTicket(ticketId, request, ownerId);

        assertThat(response.escalated()).isTrue();
        assertThat(response.status()).isEqualTo(SupportTicketStatus.ESCALATED);
        assertThat(response.escalationReason()).isEqualTo("Parcel delayed over 8 days without courier update");
        assertThat(response.escalatedAt()).isNotNull();
    }
}
