package com.dzfashion.commerce.client.impl;

import com.dzfashion.commerce.client.CatalogClient;
import com.dzfashion.commerce.client.VariantSnapshot;
import com.dzfashion.commerce.config.CommerceProperties;
import com.dzfashion.commerce.exception.ResourceNotFoundException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnMissingBean(name = "customCatalogClient")
public class DefaultCatalogClient implements CatalogClient {

    private final CommerceProperties properties;
    private final Map<UUID, VariantSnapshot> mockStore = new ConcurrentHashMap<>();

    public DefaultCatalogClient(CommerceProperties properties) {
        this.properties = properties;
    }

    public void registerVariant(VariantSnapshot snapshot) {
        mockStore.put(snapshot.variantId(), snapshot);
    }

    public void updateVariantPrice(UUID variantId, BigDecimal newPrice) {
        VariantSnapshot existing = mockStore.get(variantId);
        if (existing != null) {
            mockStore.put(variantId, new VariantSnapshot(
                    existing.variantId(),
                    existing.productId(),
                    existing.productName(),
                    existing.sku(),
                    existing.size(),
                    existing.colour(),
                    newPrice,
                    existing.active(),
                    existing.published()
            ));
        }
    }

    @Override
    public VariantSnapshot getVariant(UUID variantId) {
        VariantSnapshot snapshot = mockStore.get(variantId);
        if (snapshot != null) {
            return snapshot;
        }
        // In standalone mode if not pre-registered, return fallback or throw
        throw new ResourceNotFoundException("Variant not found in catalog: " + variantId);
    }
}
