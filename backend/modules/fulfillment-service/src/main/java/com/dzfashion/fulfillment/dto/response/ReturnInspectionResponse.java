package com.dzfashion.fulfillment.dto.response;

import com.dzfashion.fulfillment.enums.ItemDisposition;
import com.dzfashion.fulfillment.enums.ReturnInspectionResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReturnInspectionResponse(
        UUID id,
        UUID returnRequestId,
        UUID orderId,
        UUID inspectedBy,
        ReturnInspectionResult result,
        ItemDisposition disposition,
        String conditionNotes,
        List<String> photoUrls,
        Instant inspectedAt
) {
}
