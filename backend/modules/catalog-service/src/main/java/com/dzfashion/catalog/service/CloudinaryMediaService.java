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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CloudinaryMediaService {

    private static final Set<String> ALLOWED_FORMATS = Set.of("jpg", "jpeg", "png", "webp", "avif");
    private static final int MAX_DIMENSION = 5000;

    private final CloudinaryProperties properties;
    private final MediaAssetRepository mediaAssetRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    public CloudinaryMediaService(CloudinaryProperties properties,
                                  MediaAssetRepository mediaAssetRepository,
                                  ProductRepository productRepository,
                                  ProductVariantRepository productVariantRepository) {
        this.properties = properties;
        this.mediaAssetRepository = mediaAssetRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
    }

    public MediaSignatureResponse generateUploadSignature() {
        long timestamp = Instant.now().getEpochSecond();
        String folder = properties.getFolder();

        // Cloudinary expects parameters sorted alphabetically: folder=...&timestamp=...
        String toSign = String.format("folder=%s&timestamp=%d%s", folder, timestamp, properties.getApiSecret());
        String signature = computeSha1(toSign);

        return new MediaSignatureResponse(
                timestamp,
                signature,
                properties.getApiKey(),
                properties.getCloudName(),
                folder
        );
    }

    @Transactional
    public MediaAssetResponse registerMedia(UUID productId, RegisterMediaRequest request) {
        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));

        validateMediaPayload(request);

        if (request.variantId() != null) {
            boolean variantExists = productVariantRepository.existsById(request.variantId());
            if (!variantExists) {
                throw new ResourceNotFoundException("Variant not found with ID: " + request.variantId());
            }
        }

        if (request.primary()) {
            // Clear existing primary flag on other photos of this product
            List<MediaAssetEntity> existing = mediaAssetRepository.findByProductIdOrderByDisplayOrderAsc(productId);
            for (MediaAssetEntity asset : existing) {
                if (asset.isPrimary()) {
                    asset.setPrimary(false);
                    mediaAssetRepository.save(asset);
                }
            }
        }

        MediaAssetEntity entity = new MediaAssetEntity(
                product,
                request.variantId(),
                request.cloudinaryPublicId(),
                request.secureUrl(),
                request.format() != null ? request.format().toLowerCase() : null,
                request.width(),
                request.height(),
                request.altText(),
                request.displayOrder(),
                request.primary()
        );

        MediaAssetEntity saved = mediaAssetRepository.save(entity);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<MediaAssetResponse> getProductMedia(UUID productId) {
        return mediaAssetRepository.findByProductIdOrderByDisplayOrderAsc(productId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteMedia(UUID mediaId) {
        MediaAssetEntity asset = mediaAssetRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media asset not found with ID: " + mediaId));
        mediaAssetRepository.delete(asset);
    }

    private void validateMediaPayload(RegisterMediaRequest request) {
        if (request.format() != null && !ALLOWED_FORMATS.contains(request.format().toLowerCase())) {
            throw new InvalidOperationException("Unsupported media format: " + request.format() + ". Allowed: " + ALLOWED_FORMATS);
        }
        if (request.width() != null && (request.width() <= 0 || request.width() > MAX_DIMENSION)) {
            throw new InvalidOperationException("Image width must be between 1 and " + MAX_DIMENSION);
        }
        if (request.height() != null && (request.height() <= 0 || request.height() > MAX_DIMENSION)) {
            throw new InvalidOperationException("Image height must be between 1 and " + MAX_DIMENSION);
        }
        if (!request.secureUrl().startsWith("https://")) {
            throw new InvalidOperationException("Media URL must be secure HTTPS");
        }
    }

    private String computeSha1(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 algorithm not available", e);
        }
    }

    public MediaAssetResponse mapToResponse(MediaAssetEntity entity) {
        return new MediaAssetResponse(
                entity.getId(),
                entity.getVariantId(),
                entity.getCloudinaryPublicId(),
                entity.getSecureUrl(),
                entity.getFormat(),
                entity.getWidth(),
                entity.getHeight(),
                entity.getAltText(),
                entity.getDisplayOrder(),
                entity.isPrimary()
        );
    }
}
