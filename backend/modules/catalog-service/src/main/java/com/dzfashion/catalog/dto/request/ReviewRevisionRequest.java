package com.dzfashion.catalog.dto.request;

public record ReviewRevisionRequest(
        boolean approved,
        String rejectionReason
) {
}
