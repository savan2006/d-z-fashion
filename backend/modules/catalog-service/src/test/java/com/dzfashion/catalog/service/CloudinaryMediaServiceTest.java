package com.dzfashion.catalog.service;

import com.dzfashion.catalog.config.CloudinaryProperties;
import com.dzfashion.catalog.dto.request.RegisterMediaRequest;
import com.dzfashion.catalog.dto.response.MediaAssetResponse;
import com.dzfashion.catalog.dto.response.MediaSignatureResponse;
import com.dzfashion.catalog.entity.MediaAssetEntity;
import com.dzfashion.catalog.entity.ProductEntity;
import com.dzfashion.catalog.exception.InvalidOperationException;
import com.dzfashion.catalog.exception.ResourceNotFoundException;
import com.dzfashion.catalog.repository.MediaAssetRepository;
import com.dzfashion.catalog.repository.ProductRepository;
import com.dzfashion.catalog.repository.ProductVariantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloudinaryMediaServiceTest {

    @Spy
    private CloudinaryProperties properties = new CloudinaryProperties();

    @Mock
    private MediaAssetRepository mediaAssetRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @InjectMocks
    private CloudinaryMediaService mediaService;

    private UUID productId;
    private ProductEntity product;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        product = new ProductEntity(null, "Test Kurta", "test-kurta", null, null, null, BigDecimal.ZERO, UUID.randomUUID());
        product.setId(productId);
    }

    @Test
    @DisplayName("Upload signature includes timestamp, signature, apiKey, cloudName and folder")
    void generateUploadSignatureSuccess() {
        MediaSignatureResponse signature = mediaService.generateUploadSignature();

        assertThat(signature).isNotNull();
        assertThat(signature.timestamp()).isPositive();
        assertThat(signature.signature()).isNotBlank();
        assertThat(signature.apiKey()).isEqualTo("mock-key");
        assertThat(signature.cloudName()).isEqualTo("dzfashion");
        assertThat(signature.folder()).isEqualTo("dzfashion/catalog");
    }

    @Test
    @DisplayName("Register media succeeds with valid HTTPS URL and allowed format")
    void registerMediaSuccess() {
        RegisterMediaRequest request = new RegisterMediaRequest(
                "dzfashion/catalog/kurta1",
                "https://res.cloudinary.com/dzfashion/image/upload/v1/kurta1.jpg",
                "jpg",
                1200,
                1600,
                "Maroon Kurta front view",
                1,
                true,
                null
        );

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(mediaAssetRepository.findByProductIdOrderByDisplayOrderAsc(productId)).thenReturn(List.of());
        when(mediaAssetRepository.save(any(MediaAssetEntity.class))).thenAnswer(invocation -> {
            MediaAssetEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        MediaAssetResponse response = mediaService.registerMedia(productId, request);

        assertThat(response).isNotNull();
        assertThat(response.cloudinaryPublicId()).isEqualTo("dzfashion/catalog/kurta1");
        assertThat(response.format()).isEqualTo("jpg");
        assertThat(response.primary()).isTrue();
    }

    @Test
    @DisplayName("Register media rejects non-HTTPS URL")
    void registerMediaRejectsInsecureUrl() {
        RegisterMediaRequest request = new RegisterMediaRequest(
                "dzfashion/catalog/kurta1",
                "http://insecure.url/kurta1.jpg",
                "jpg",
                800,
                800,
                "Kurta",
                1,
                false,
                null
        );

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> mediaService.registerMedia(productId, request))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("must be secure HTTPS");
    }

    @Test
    @DisplayName("Register media rejects unsupported file format")
    void registerMediaRejectsInvalidFormat() {
        RegisterMediaRequest request = new RegisterMediaRequest(
                "dzfashion/catalog/script",
                "https://res.cloudinary.com/dzfashion/script.exe",
                "exe",
                800,
                800,
                "Dangerous file",
                1,
                false,
                null
        );

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> mediaService.registerMedia(productId, request))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Unsupported media format");
    }

    @Test
    @DisplayName("Register media rejects invalid dimensions")
    void registerMediaRejectsExcessiveDimensions() {
        RegisterMediaRequest request = new RegisterMediaRequest(
                "dzfashion/catalog/giant",
                "https://res.cloudinary.com/dzfashion/giant.png",
                "png",
                10000,
                10000,
                "Giant file",
                1,
                false,
                null
        );

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> mediaService.registerMedia(productId, request))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("must be between 1 and 5000");
    }
}
