package com.dzfashion.commerce.dto.response;

public record ManagerOperationalReportResponse(
        long totalOrders,
        long pendingClaimOrders,
        long claimedOrders,
        long inPickingOrders,
        long dispatchedOrders,
        long deliveredOrders,
        long openSupportTickets,
        long escalatedSupportTickets,
        long reportedShortages
) {
}
