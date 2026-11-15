package com.dzfashion.catalog.service;

import com.dzfashion.catalog.dto.request.CreateProductDraftRequest;
import com.dzfashion.catalog.dto.request.CreateProductOwnerRequest;
import com.dzfashion.catalog.dto.request.CreateVariantDraftRequest;
import com.dzfashion.catalog.dto.request.CreateVariantRequest;
import com.dzfashion.catalog.dto.request.UpdateProductPriceRequest;
import com.dzfashion.catalog.dto.response.ProductResponse;
import com.dzfashion.catalog.entity.CategoryEntity;
import com.dzfashion.catalog.entity.ProductEntity;
import com.dzfashion.catalog.enums.ProductStatus;
import com.dzfashion.catalog.exception.InvalidOperationException;
import com.dzfashion.catalog.exception.ResourceConflictException;
import com.dzfashion.catalog.repository.CategoryRepository;
import com.dzfashion.catalog.repository.ProductRepository;
import com.dzfashion.catalog.repository.ProductVariantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private SkuGeneratorService skuGeneratorService;

    @Mock
    private CloudinaryMediaService mediaService;

    @InjectMocks
    private ProductService productService;

    private CategoryEntity testCategory;
    private UUID categoryId;
    private UUID managerId;
    private UUID ownerId;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();
        managerId = UUID.randomUUID();
        ownerId = UUID.randomUUID();

        testCategory = new CategoryEntity("Kurtas", "kurtas", "Women Kurtas", null, 1);
        testCategory.setId(categoryId);
    }

    @Test
    @DisplayName("Manager creates draft product: price is 0 and status is DRAFT")
    void createProductDraftSuccess() {
        CreateProductDraftRequest request = new CreateProductDraftRequest(
                "Anarkali Kurta",
                "anarkali-kurta",
                categoryId,
                "Pure cotton anarkali",
                "Cotton",
                "Festive",
                List.of(new CreateVariantDraftRequest("M", "Maroon")),
                List.of()
        );

        when(productRepository.existsBySlug("anarkali-kurta")).thenReturn(false);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(testCategory));
        when(skuGeneratorService.generateUniqueSku(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("DZF-KUR-ANAR-MAR-M-001");
        when(productRepository.save(any(ProductEntity.class))).thenAnswer(invocation -> {
            ProductEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        ProductResponse response = productService.createProductDraft(request, managerId);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Anarkali Kurta");
        assertThat(response.status()).isEqualTo(ProductStatus.DRAFT);
        assertThat(response.defaultPrice()).isEqualTo(BigDecimal.ZERO);
        assertThat(response.variants()).hasSize(1);
        assertThat(response.variants().get(0).sku()).isEqualTo("DZF-KUR-ANAR-MAR-M-001");
    }

    @Test
    @DisplayName("Create draft rejects duplicate slug")
    void createProductDraftDuplicateSlug() {
        CreateProductDraftRequest request = new CreateProductDraftRequest(
                "Anarkali Kurta",
                "anarkali-kurta",
                categoryId,
                null, null, null, List.of(), List.of()
        );

        when(productRepository.existsBySlug("anarkali-kurta")).thenReturn(true);

        assertThatThrownBy(() -> productService.createProductDraft(request, managerId))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Product slug already exists");
    }

    @Test
    @DisplayName("Owner creates product directly with price and immediate publication")
    void createProductOwnerSuccess() {
        CreateProductOwnerRequest request = new CreateProductOwnerRequest(
                "Silk Saree",
                "silk-saree",
                categoryId,
                "Surat silk saree",
                "Silk",
                "Wedding",
                new BigDecimal("2499.00"),
                true,
                List.of(new CreateVariantRequest("Free", "Red", null)),
                List.of()
        );

        when(productRepository.existsBySlug("silk-saree")).thenReturn(false);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(testCategory));
        when(skuGeneratorService.generateUniqueSku(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("DZF-KUR-SILK-RED-FREE-001");
        when(productRepository.save(any(ProductEntity.class))).thenAnswer(invocation -> {
            ProductEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        ProductResponse response = productService.createProductOwner(request, ownerId);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(ProductStatus.PUBLISHED);
        assertThat(response.defaultPrice()).isEqualByComparingTo("2499.00");
    }

    @Test
    @DisplayName("Owner updates product price successfully")
    void updateProductPriceSuccess() {
        UUID productId = UUID.randomUUID();
        ProductEntity product = new ProductEntity(testCategory, "Kurta", "kurta", null, null, null, BigDecimal.ZERO, managerId);
        product.setId(productId);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(ProductEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProductPriceRequest request = new UpdateProductPriceRequest(new BigDecimal("1299.00"));
        ProductResponse response = productService.updateProductPrice(productId, request, ownerId);

        assertThat(response.defaultPrice()).isEqualByComparingTo("1299.00");
    }

    @Test
    @DisplayName("Publishing product fails if price is zero")
    void publishProductWithoutPriceFails() {
        UUID productId = UUID.randomUUID();
        ProductEntity product = new ProductEntity(testCategory, "Kurta", "kurta", null, null, null, BigDecimal.ZERO, managerId);
        product.setId(productId);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.publishProduct(productId, ownerId))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Cannot publish product without a valid price");
    }

    @Test
    @DisplayName("Publishing product succeeds when valid price is set")
    void publishProductSuccess() {
        UUID productId = UUID.randomUUID();
        ProductEntity product = new ProductEntity(testCategory, "Kurta", "kurta", null, null, null, new BigDecimal("899.00"), managerId);
        product.setId(productId);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(ProductEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse response = productService.publishProduct(productId, ownerId);

        assertThat(response.status()).isEqualTo(ProductStatus.PUBLISHED);
    }

    @Test
    @DisplayName("Archiving product sets status to ARCHIVED")
    void archiveProductSuccess() {
        UUID productId = UUID.randomUUID();
        ProductEntity product = new ProductEntity(testCategory, "Kurta", "kurta", null, null, null, new BigDecimal("899.00"), managerId);
        product.setId(productId);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        productService.archiveProduct(productId, ownerId);

        assertThat(product.getStatus()).isEqualTo(ProductStatus.ARCHIVED);
        verify(productRepository).save(product);
    }
}
