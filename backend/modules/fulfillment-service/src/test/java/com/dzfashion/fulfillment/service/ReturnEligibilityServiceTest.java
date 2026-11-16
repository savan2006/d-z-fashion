package com.dzfashion.fulfillment.service;

import com.dzfashion.fulfillment.config.FulfillmentProperties;
import com.dzfashion.fulfillment.dto.response.ReturnEligibilityResponse;
import com.dzfashion.fulfillment.entity.ShipmentEntity;
import com.dzfashion.fulfillment.entity.ShipmentItemEntity;
import com.dzfashion.fulfillment.enums.ShipmentStatus;
import com.dzfashion.fulfillment.repository.ReturnRequestRepository;
import com.dzfashion.fulfillment.repository.ShipmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReturnEligibilityServiceTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private ReturnRequestRepository returnRequestRepository;

    @Mock
    private FulfillmentProperties fulfillmentProperties;

    @InjectMocks
    private ReturnEligibilityService returnEligibilityService;

    private UUID orderId;
    private UUID orderItemId;
    private ShipmentEntity shipment;

    @BeforeEach
    void setUp() {
        lenient().when(fulfillmentProperties.getReturnWindowDays()).thenReturn(3);

        orderId = UUID.randomUUID();
        orderItemId = UUID.randomUUID();

        shipment = new ShipmentEntity(
                orderId, "ORD-20261116-0001", UUID.randomUUID(), "Fatima Sheikh",
                "9876543210", "Surat Ring Road", "Surat", "Gujarat", "395002", true, new BigDecimal("1999.00")
        );
        shipment.setStatus(ShipmentStatus.DELIVERED);
        shipment.setDeliveryDate(LocalDate.now());
        shipment.setEligibleReturnUntil(Instant.now().plus(3, ChronoUnit.DAYS));

        ShipmentItemEntity item = new ShipmentItemEntity(
                orderItemId, UUID.randomUUID(), UUID.randomUUID(), "Silk Kurti", "KURTI-01", "M", "Navy", "BIN-1", 2
        );
        shipment.addItem(item);
    }

    @Test
    @DisplayName("Delivered order within 3-day window should be eligible for return")
    void testCheckEligibility_Eligible() {
        when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.of(shipment));
        when(returnRequestRepository.countActiveReturnQuantityByOrderItemId(orderItemId)).thenReturn(0);

        ReturnEligibilityResponse response = returnEligibilityService.checkEligibility(orderId);

        assertThat(response.eligible()).isTrue();
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).eligibleQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("Non-delivered shipment should not be eligible for return")
    void testCheckEligibility_NotDelivered() {
        shipment.setStatus(ShipmentStatus.DISPATCHED);
        when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.of(shipment));

        ReturnEligibilityResponse response = returnEligibilityService.checkEligibility(orderId);

        assertThat(response.eligible()).isFalse();
        assertThat(response.reasonIfNotEligible()).contains("has not been delivered yet");
    }

    @Test
    @DisplayName("Expired 3-day window should not be eligible for return")
    void testCheckEligibility_WindowExpired() {
        shipment.setEligibleReturnUntil(Instant.now().minus(1, ChronoUnit.HOURS)); // Expired!
        when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.of(shipment));

        ReturnEligibilityResponse response = returnEligibilityService.checkEligibility(orderId);

        assertThat(response.eligible()).isFalse();
        assertThat(response.reasonIfNotEligible()).contains("Return window expired");
    }

    @Test
    @DisplayName("Order with all items already returned should not have eligible quantity")
    void testCheckEligibility_AllAlreadyReturned() {
        when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.of(shipment));
        when(returnRequestRepository.countActiveReturnQuantityByOrderItemId(orderItemId)).thenReturn(2); // All 2 active/returned

        ReturnEligibilityResponse response = returnEligibilityService.checkEligibility(orderId);

        assertThat(response.eligible()).isFalse();
        assertThat(response.reasonIfNotEligible()).contains("All items from this delivered order have already been returned");
    }
}
