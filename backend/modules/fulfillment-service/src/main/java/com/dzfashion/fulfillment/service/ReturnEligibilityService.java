package com.dzfashion.fulfillment.service;

import com.dzfashion.fulfillment.config.FulfillmentProperties;
import com.dzfashion.fulfillment.dto.response.ReturnEligibilityResponse;
import com.dzfashion.fulfillment.entity.ShipmentEntity;
import com.dzfashion.fulfillment.entity.ShipmentItemEntity;
import com.dzfashion.fulfillment.enums.ShipmentStatus;
import com.dzfashion.fulfillment.exception.ResourceNotFoundException;
import com.dzfashion.fulfillment.repository.ReturnRequestRepository;
import com.dzfashion.fulfillment.repository.ShipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ReturnEligibilityService {

    private final ShipmentRepository shipmentRepository;
    private final ReturnRequestRepository returnRequestRepository;
    private final FulfillmentProperties fulfillmentProperties;

    public ReturnEligibilityService(ShipmentRepository shipmentRepository,
                                    ReturnRequestRepository returnRequestRepository,
                                    FulfillmentProperties fulfillmentProperties) {
        this.shipmentRepository = shipmentRepository;
        this.returnRequestRepository = returnRequestRepository;
        this.fulfillmentProperties = fulfillmentProperties;
    }

    public ReturnEligibilityResponse checkEligibility(UUID orderId) {
        ShipmentEntity shipment = shipmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found for order ID: " + orderId));

        if (shipment.getStatus() != ShipmentStatus.DELIVERED) {
            return new ReturnEligibilityResponse(
                    shipment.getOrderId(),
                    shipment.getOrderNumber(),
                    false,
                    null,
                    shipment.getDeliveryDate(),
                    "Order has not been delivered yet. Current status: " + shipment.getStatus(),
                    List.of()
            );
        }

        Instant eligibleUntil = shipment.getEligibleReturnUntil();
        if (eligibleUntil == null && shipment.getDeliveryDate() != null) {
            // Fallback: 3 days after delivery date
            eligibleUntil = shipment.getCreatedAt().plus(fulfillmentProperties.getReturnWindowDays(), ChronoUnit.DAYS);
        }

        Instant now = Instant.now();
        if (eligibleUntil == null || now.isAfter(eligibleUntil)) {
            return new ReturnEligibilityResponse(
                    shipment.getOrderId(),
                    shipment.getOrderNumber(),
                    false,
                    eligibleUntil,
                    shipment.getDeliveryDate(),
                    "Return window expired. Under D&Z Fashion quality policy, return/exchange requests are only accepted within 3 days of delivery.",
                    List.of()
            );
        }

        List<ReturnEligibilityResponse.ReturnEligibleItem> eligibleItems = new ArrayList<>();
        boolean hasEligibleQuantity = false;

        for (ShipmentItemEntity item : shipment.getItems()) {
            int activeQuantity = returnRequestRepository.countActiveReturnQuantityByOrderItemId(item.getOrderItemId());
            int eligibleQty = Math.max(0, item.getQuantity() - activeQuantity);

            if (eligibleQty > 0) {
                hasEligibleQuantity = true;
            }

            eligibleItems.add(new ReturnEligibilityResponse.ReturnEligibleItem(
                    item.getOrderItemId(),
                    item.getSku(),
                    item.getProductName(),
                    item.getQuantity(),
                    activeQuantity,
                    eligibleQty,
                    java.math.BigDecimal.ZERO // Placeholder if item level price is queried
            ));
        }

        if (!hasEligibleQuantity) {
            return new ReturnEligibilityResponse(
                    shipment.getOrderId(),
                    shipment.getOrderNumber(),
                    false,
                    eligibleUntil,
                    shipment.getDeliveryDate(),
                    "All items from this delivered order have already been returned, exchanged, or have pending requests.",
                    eligibleItems
            );
        }

        return new ReturnEligibilityResponse(
                shipment.getOrderId(),
                shipment.getOrderNumber(),
                true,
                eligibleUntil,
                shipment.getDeliveryDate(),
                null,
                eligibleItems
        );
    }
}
