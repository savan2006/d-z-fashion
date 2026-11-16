package com.dzfashion.fulfillment.enums;

public enum ShipmentStatus {
    PROCESSING,
    DISPATCHED,
    OUT_FOR_DELIVERY,
    DELIVERY_ATTEMPTED_1,
    DELIVERY_ATTEMPTED_2,
    RETURNING_TO_WAREHOUSE,
    RETURNED_TO_WAREHOUSE,
    DELIVERED;

    public CustomerShipmentStatus toCustomerStatus() {
        return switch (this) {
            case PROCESSING -> CustomerShipmentStatus.PROCESSING;
            case DISPATCHED, OUT_FOR_DELIVERY, DELIVERY_ATTEMPTED_1, DELIVERY_ATTEMPTED_2 -> CustomerShipmentStatus.SHIPPED;
            case DELIVERED -> CustomerShipmentStatus.DELIVERED;
            case RETURNING_TO_WAREHOUSE -> CustomerShipmentStatus.RETURNING_TO_WAREHOUSE;
            case RETURNED_TO_WAREHOUSE -> CustomerShipmentStatus.RETURNED_TO_WAREHOUSE;
        };
    }
}
