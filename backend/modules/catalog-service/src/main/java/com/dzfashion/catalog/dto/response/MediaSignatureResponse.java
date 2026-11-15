package com.dzfashion.catalog.dto.response;

public record MediaSignatureResponse(
        long timestamp,
        String signature,
        String apiKey,
        String cloudName,
        String folder
) {
}
