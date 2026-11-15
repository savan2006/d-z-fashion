package com.dzfashion.catalog.service;

import com.dzfashion.catalog.repository.ProductVariantRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkuGeneratorServiceTest {

    @Mock
    private ProductVariantRepository productVariantRepository;

    @InjectMocks
    private SkuGeneratorService skuGeneratorService;

    @Test
    @DisplayName("Generated SKU follows DZF format and contains category, product, colour, size")
    void generateUniqueSkuStandard() {
        when(productVariantRepository.existsBySku(anyString())).thenReturn(false);

        String sku = skuGeneratorService.generateUniqueSku("Women Kurtas", "embroidered-kurta-set", "Maroon", "M");

        assertThat(sku).startsWith("DZF-WOM-EMBR-MAR-M-");
        assertThat(sku.split("-")).hasSize(6);
    }

    @Test
    @DisplayName("Handles collision by generating an alternative unique suffix")
    void generateUniqueSkuWithCollision() {
        // First candidate exists, second does not
        when(productVariantRepository.existsBySku(anyString()))
                .thenReturn(true)
                .thenReturn(false);

        String sku = skuGeneratorService.generateUniqueSku("Kurtas", "anarkali-suit", "Navy", "XL");

        assertThat(sku).startsWith("DZF-KUR-ANAR-NAV-XL-");
    }

    @Test
    @DisplayName("Handles null or short category and size names gracefully")
    void generateUniqueSkuShortInputs() {
        when(productVariantRepository.existsBySku(anyString())).thenReturn(false);

        String sku = skuGeneratorService.generateUniqueSku("", "", "", "");

        assertThat(sku).startsWith("DZF-GEN-GEN-GEN-STD-");
    }
}
