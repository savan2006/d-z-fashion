package com.dzfashion.catalog.service;

import com.dzfashion.catalog.repository.ProductVariantRepository;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Locale;

@Service
public class SkuGeneratorService {

    private static final String PREFIX = "DZF";
    private static final String ALPHANUMERIC = "0123456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ProductVariantRepository productVariantRepository;

    public SkuGeneratorService(ProductVariantRepository productVariantRepository) {
        this.productVariantRepository = productVariantRepository;
    }

    public String generateUniqueSku(String categoryName, String productSlug, String colour, String size) {
        String categoryPart = extractAbbreviation(categoryName, 3);
        String productPart = extractAbbreviation(productSlug.replace("-", " "), 4);
        String colourPart = extractAbbreviation(colour, 3);
        String sizePart = cleanSize(size);

        for (int attempt = 0; attempt < 50; attempt++) {
            String suffix = generateRandomSuffix(3);
            String candidateSku = String.format("%s-%s-%s-%s-%s-%s",
                    PREFIX, categoryPart, productPart, colourPart, sizePart, suffix)
                    .toUpperCase(Locale.ROOT);

            if (!productVariantRepository.existsBySku(candidateSku)) {
                return candidateSku;
            }
        }

        // Fallback with timestamp to guarantee uniqueness
        return String.format("%s-%s-%s-%s-%s-%s",
                PREFIX, categoryPart, productPart, colourPart, sizePart, Long.toHexString(System.currentTimeMillis()).toUpperCase(Locale.ROOT));
    }

    private String extractAbbreviation(String input, int length) {
        if (input == null || input.isBlank()) {
            return "GEN";
        }
        String cleaned = input.replaceAll("[^a-zA-Z0-9]", "").toUpperCase(Locale.ROOT);
        if (cleaned.length() <= length) {
            return cleaned;
        }
        return cleaned.substring(0, length);
    }

    private String cleanSize(String size) {
        if (size == null || size.isBlank()) {
            return "STD";
        }
        return size.replaceAll("[^a-zA-Z0-9]", "").toUpperCase(Locale.ROOT);
    }

    private String generateRandomSuffix(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = RANDOM.nextInt(ALPHANUMERIC.length());
            sb.append(ALPHANUMERIC.charAt(index));
        }
        return sb.toString();
    }
}
