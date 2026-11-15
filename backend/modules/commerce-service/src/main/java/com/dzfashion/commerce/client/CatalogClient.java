package com.dzfashion.commerce.client;

import java.util.UUID;

public interface CatalogClient {
    VariantSnapshot getVariant(UUID variantId);
}
