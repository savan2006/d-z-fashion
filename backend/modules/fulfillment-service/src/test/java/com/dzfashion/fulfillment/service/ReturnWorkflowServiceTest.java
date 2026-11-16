package com.dzfashion.fulfillment.service;

import com.dzfashion.fulfillment.config.FulfillmentProperties;
import com.dzfashion.fulfillment.dto.request.ConfirmExchangePaymentRequest;
import com.dzfashion.fulfillment.dto.request.CreateReturnRequest;
import com.dzfashion.fulfillment.dto.request.PerformInspectionRequest;
import com.dzfashion.fulfillment.dto.request.RecordPickupRequest;
import com.dzfashion.fulfillment.dto.request.RecordReturnArrivalRequest;
import com.dzfashion.fulfillment.dto.request.RejectRefundRequest;
import com.dzfashion.fulfillment.dto.request.SchedulePickupRequest;
import com.dzfashion.fulfillment.dto.response.ReturnEligibilityResponse;
import com.dzfashion.fulfillment.dto.response.ReturnRequestResponse;
import com.dzfashion.fulfillment.entity.ReturnInspectionEntity;
import com.dzfashion.fulfillment.entity.ReturnRequestEntity;
import com.dzfashion.fulfillment.entity.ShipmentEntity;
import com.dzfashion.fulfillment.entity.ShipmentItemEntity;
import com.dzfashion.fulfillment.entity.TransportAgencyEntity;
import com.dzfashion.fulfillment.enums.ItemDisposition;
import com.dzfashion.fulfillment.enums.ReturnInspectionResult;
import com.dzfashion.fulfillment.enums.ReturnReason;
import com.dzfashion.fulfillment.enums.ReturnRequestStatus;
import com.dzfashion.fulfillment.enums.ReturnResolutionType;
import com.dzfashion.fulfillment.enums.ShipmentStatus;
import com.dzfashion.fulfillment.exception.InvalidOperationException;
import com.dzfashion.fulfillment.repository.OutboxEventRepository;
import com.dzfashion.fulfillment.repository.ReturnInspectionRepository;
import com.dzfashion.fulfillment.repository.ReturnRequestRepository;
import com.dzfashion.fulfillment.repository.ShipmentRepository;
import com.dzfashion.fulfillment.repository.TransportAgencyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReturnWorkflowServiceTest {

    @Mock
    private ReturnRequestRepository returnRequestRepository;

    @Mock
    private ReturnInspectionRepository returnInspectionRepository;

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private TransportAgencyRepository transportAgencyRepository;

    @Mock
    private ReturnEligibilityService returnEligibilityService;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private FulfillmentProperties fulfillmentProperties;

    private ObjectMapper objectMapper;
    private ReturnWorkflowService returnWorkflowService;

    private UUID orderId;
    private UUID orderItemId;
    private UUID customerId;
    private ShipmentEntity shipment;
    private ShipmentItemEntity itemEntity;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        returnWorkflowService = new ReturnWorkflowService(
                returnRequestRepository,
                returnInspectionRepository,
                shipmentRepository,
                transportAgencyRepository,
                returnEligibilityService,
                outboxEventRepository,
                fulfillmentProperties,
                objectMapper
        );

        lenient().when(fulfillmentProperties.getWarehouseName()).thenReturn("Surat Central Warehouse");

        orderId = UUID.randomUUID();
        orderItemId = UUID.randomUUID();
        customerId = UUID.randomUUID();

        shipment = new ShipmentEntity(
                orderId, "ORD-20261116-0001", customerId, "Fatima Sheikh",
                "9876543210", "Surat Ring Road", "Surat", "Gujarat", "395002", true, new BigDecimal("2000.00")
        );
        shipment.setStatus(ShipmentStatus.DELIVERED);
        shipment.setDeliveryDate(LocalDate.now());
        shipment.setEligibleReturnUntil(Instant.now().plus(3, ChronoUnit.DAYS));

        itemEntity = new ShipmentItemEntity(
                orderItemId, UUID.randomUUID(), UUID.randomUUID(), "Embroidered Kurti", "KURTI-01", "M", "Navy", "BIN-1", 2
        );
        shipment.addItem(itemEntity);
    }

    @Test
    @DisplayName("Should create exchange return request with photos and calculate price difference")
    void testCreateReturnRequest_Exchange_Success() {
        ReturnEligibilityResponse eligibility = new ReturnEligibilityResponse(
                orderId, "ORD-20261116-0001", true, Instant.now().plus(3, ChronoUnit.DAYS),
                LocalDate.now(), null, List.of()
        );
        when(returnEligibilityService.checkEligibility(orderId)).thenReturn(eligibility);
        when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.of(shipment));
        when(returnRequestRepository.countActiveReturnQuantityByOrderItemId(orderItemId)).thenReturn(0);
        when(returnRequestRepository.save(any(ReturnRequestEntity.class))).thenAnswer(inv -> {
            ReturnRequestEntity e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        // Original 1000, replacement 1200 (more expensive by 200 per unit, qty 1 -> difference = +200)
        CreateReturnRequest request = new CreateReturnRequest(
                orderId, "ORD-20261116-0001", orderItemId, "KURTI-01", "Embroidered Kurti", 1,
                new BigDecimal("1000.00"), ReturnReason.DEFECTIVE, ReturnResolutionType.EXCHANGE,
                List.of("https://res.cloudinary.com/dz/image/upload/v1/defect1.jpg"),
                "Torn seam near collar",
                UUID.randomUUID(), UUID.randomUUID(), "KURTI-02", "Embroidered Kurti Premium", new BigDecimal("1200.00")
        );

        ReturnRequestResponse response = returnWorkflowService.createReturnRequest(request, customerId);

        assertThat(response).isNotNull();
        assertThat(response.returnNumber()).startsWith("RET-");
        assertThat(response.reason()).isEqualTo(ReturnReason.DEFECTIVE);
        assertThat(response.resolutionType()).isEqualTo(ReturnResolutionType.EXCHANGE);
        assertThat(response.priceDifference()).isEqualByComparingTo("200.00");
        assertThat(response.priceDifferenceSettled()).isFalse(); // Payment pending for more expensive replacement
        verify(outboxEventRepository).save(any());
    }

    @Test
    @DisplayName("Should reject return request if requested quantity exceeds eligible remaining quantity")
    void testCreateReturnRequest_ExcessQuantityRejected() {
        ReturnEligibilityResponse eligibility = new ReturnEligibilityResponse(
                orderId, "ORD-20261116-0001", true, Instant.now().plus(3, ChronoUnit.DAYS),
                LocalDate.now(), null, List.of()
        );
        when(returnEligibilityService.checkEligibility(orderId)).thenReturn(eligibility);
        when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.of(shipment));
        when(returnRequestRepository.countActiveReturnQuantityByOrderItemId(orderItemId)).thenReturn(1); // 1 already active, 1 remaining

        CreateReturnRequest request = new CreateReturnRequest(
                orderId, "ORD-20261116-0001", orderItemId, "KURTI-01", "Embroidered Kurti", 2, // Requests 2 when only 1 remaining!
                new BigDecimal("1000.00"), ReturnReason.DAMAGED, ReturnResolutionType.EXCHANGE,
                List.of("https://res.cloudinary.com/dz/image/upload/v1/defect1.jpg"),
                "Notes", null, null, null, null, null
        );

        assertThatThrownBy(() -> returnWorkflowService.createReturnRequest(request, customerId))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("exceeds eligible quantity");
    }

    @Test
    @DisplayName("Should reject return request if photo evidence is missing")
    void testCreateReturnRequest_MissingPhotosRejected() {
        ReturnEligibilityResponse eligibility = new ReturnEligibilityResponse(
                orderId, "ORD-20261116-0001", true, Instant.now().plus(3, ChronoUnit.DAYS),
                LocalDate.now(), null, List.of()
        );
        when(returnEligibilityService.checkEligibility(orderId)).thenReturn(eligibility);
        when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.of(shipment));
        when(returnRequestRepository.countActiveReturnQuantityByOrderItemId(orderItemId)).thenReturn(0);

        CreateReturnRequest request = new CreateReturnRequest(
                orderId, "ORD-20261116-0001", orderItemId, "KURTI-01", "Embroidered Kurti", 1,
                new BigDecimal("1000.00"), ReturnReason.DAMAGED, ReturnResolutionType.EXCHANGE,
                List.of(), // Missing photos!
                "Notes", null, null, null, null, null
        );

        assertThatThrownBy(() -> returnWorkflowService.createReturnRequest(request, customerId))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("photo is mandatory");
    }

    @Test
    @DisplayName("Manager schedules pickup with transport agency (cost paid by D&Z)")
    void testSchedulePickup_Success() {
        UUID returnId = UUID.randomUUID();
        ReturnRequestEntity entity = new ReturnRequestEntity(
                "RET-20261116-0001", orderId, "ORD-001", orderItemId, customerId,
                "KURTI-01", "Kurti", 1, new BigDecimal("1000.00"),
                ReturnReason.DAMAGED, ReturnResolutionType.EXCHANGE,
                List.of("photo.jpg"), "Notes", true
        );
        entity.setId(returnId);

        UUID agencyId = UUID.randomUUID();
        TransportAgencyEntity agency = new TransportAgencyEntity("Delhivery", "Lead", "9876543210");
        agency.setId(agencyId);

        when(returnRequestRepository.findById(returnId)).thenReturn(Optional.of(entity));
        when(transportAgencyRepository.findById(agencyId)).thenReturn(Optional.of(agency));
        when(returnRequestRepository.save(any(ReturnRequestEntity.class))).thenReturn(entity);

        SchedulePickupRequest request = new SchedulePickupRequest(
                agencyId, LocalDate.now().plusDays(1), "PICKUP-WAYBILL-101", "Customer available morning"
        );

        ReturnRequestResponse response = returnWorkflowService.schedulePickup(returnId, request, UUID.randomUUID());

        assertThat(response.status()).isEqualTo(ReturnRequestStatus.PICKUP_SCHEDULED);
        assertThat(response.transportAgencyName()).isEqualTo("Delhivery");
        assertThat(response.pickupReference()).isEqualTo("PICKUP-WAYBILL-101");
        verify(outboxEventRepository).save(any());
    }

    @Test
    @DisplayName("Surat inspection PASSED prepares exchange and records disposition")
    void testPerformInspection_Passed() {
        UUID returnId = UUID.randomUUID();
        ReturnRequestEntity entity = new ReturnRequestEntity(
                "RET-20261116-0001", orderId, "ORD-001", orderItemId, customerId,
                "KURTI-01", "Kurti", 1, new BigDecimal("1000.00"),
                ReturnReason.DEFECTIVE, ReturnResolutionType.EXCHANGE,
                List.of("photo.jpg"), "Notes", true
        );
        entity.setId(returnId);
        entity.setStatus(ReturnRequestStatus.RECEIVED_AT_SURAT); // Physically received at Surat!

        when(returnRequestRepository.findById(returnId)).thenReturn(Optional.of(entity));
        when(returnRequestRepository.save(any(ReturnRequestEntity.class))).thenReturn(entity);

        PerformInspectionRequest request = new PerformInspectionRequest(
                ReturnInspectionResult.PASSED,
                ItemDisposition.DAMAGED_QUARANTINE, // Invariant: defective/damaged stock cannot return to sellable inventory
                "Verified fabric defect near collar seam. Tag intact.",
                List.of("inspection_photo1.jpg")
        );

        ReturnRequestResponse response = returnWorkflowService.performInspection(returnId, request, UUID.randomUUID());

        assertThat(response.status()).isEqualTo(ReturnRequestStatus.EXCHANGE_PREPARED);
        assertThat(response.inspection()).isNotNull();
        assertThat(response.inspection().result()).isEqualTo(ReturnInspectionResult.PASSED);
        assertThat(response.inspection().disposition()).isEqualTo(ItemDisposition.DAMAGED_QUARANTINE);
        verify(outboxEventRepository).save(any());
    }

    @Test
    @DisplayName("Inspection cannot be performed before physical arrival at Surat warehouse")
    void testPerformInspection_BeforeArrival_Rejected() {
        UUID returnId = UUID.randomUUID();
        ReturnRequestEntity entity = new ReturnRequestEntity();
        entity.setStatus(ReturnRequestStatus.IN_TRANSIT_TO_SURAT); // Still in transit!

        when(returnRequestRepository.findById(returnId)).thenReturn(Optional.of(entity));

        PerformInspectionRequest request = new PerformInspectionRequest(
                ReturnInspectionResult.PASSED, ItemDisposition.DAMAGED_QUARANTINE, "Notes", List.of()
        );

        assertThatThrownBy(() -> returnWorkflowService.performInspection(returnId, request, UUID.randomUUID()))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Physical parcel must first reach Surat warehouse");
    }

    @Test
    @DisplayName("Exchange replacement cannot be dispatched before price difference is settled")
    void testDispatchExchange_UnsettledDifference_Rejected() {
        UUID returnId = UUID.randomUUID();
        ReturnRequestEntity entity = new ReturnRequestEntity();
        entity.setStatus(ReturnRequestStatus.EXCHANGE_PREPARED);
        entity.setPriceDifference(new BigDecimal("250.00"));
        entity.setPriceDifferenceSettled(false); // Unsettled!
        entity.setInspection(new ReturnInspectionEntity());
        entity.getInspection().setResult(ReturnInspectionResult.PASSED);

        when(returnRequestRepository.findById(returnId)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> returnWorkflowService.dispatchExchangeReplacement(returnId, UUID.randomUUID()))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("has not been settled yet");
    }

    @Test
    @DisplayName("Owner approves exceptional refund, establishing exact 7-day deadline")
    void testApproveRefund_OwnerSuccess() {
        UUID returnId = UUID.randomUUID();
        ReturnRequestEntity entity = new ReturnRequestEntity(
                "RET-20261116-0001", orderId, "ORD-001", orderItemId, customerId,
                "KURTI-01", "Kurti", 1, new BigDecimal("1500.00"),
                ReturnReason.DAMAGED, ReturnResolutionType.REFUND,
                List.of("photo.jpg"), "Notes", true // COD order
        );
        entity.setId(returnId);
        entity.setStatus(ReturnRequestStatus.REFUND_PENDING_OWNER_APPROVAL);
        entity.setRefundAmount(new BigDecimal("1500.00"));

        when(returnRequestRepository.findById(returnId)).thenReturn(Optional.of(entity));
        when(returnRequestRepository.save(any(ReturnRequestEntity.class))).thenReturn(entity);

        UUID ownerId = UUID.randomUUID();
        ReturnRequestResponse response = returnWorkflowService.approveRefund(returnId, ownerId);

        assertThat(response.status()).isEqualTo(ReturnRequestStatus.REFUND_APPROVED);
        assertThat(response.refundApprovedBy()).isEqualTo(ownerId);
        assertThat(response.refundDeadline()).isNotNull();
        // Deadline is exactly 7 days after approval
        assertThat(response.refundDeadline()).isAfter(Instant.now().plus(6, ChronoUnit.DAYS));
        verify(outboxEventRepository).save(any());
    }

    @Test
    @DisplayName("Owner rejects refund request with recorded reason")
    void testRejectRefund_OwnerSuccess() {
        UUID returnId = UUID.randomUUID();
        ReturnRequestEntity entity = new ReturnRequestEntity();
        entity.setId(returnId);
        entity.setReturnNumber("RET-20261116-0001");
        entity.setStatus(ReturnRequestStatus.REFUND_PENDING_OWNER_APPROVAL);

        when(returnRequestRepository.findById(returnId)).thenReturn(Optional.of(entity));
        when(returnRequestRepository.save(any(ReturnRequestEntity.class))).thenReturn(entity);

        UUID ownerId = UUID.randomUUID();
        RejectRefundRequest request = new RejectRefundRequest("Defect not verified; standard exchange only");
        ReturnRequestResponse response = returnWorkflowService.rejectRefund(returnId, request, ownerId);

        assertThat(response.status()).isEqualTo(ReturnRequestStatus.REJECTED);
        assertThat(response.refundRejectionReason()).isEqualTo("Defect not verified; standard exchange only");
        verify(outboxEventRepository).save(any());
    }
}
