package com.dzfashion.catalog.service;

import com.dzfashion.catalog.dto.request.CreateMediaRequest;
import com.dzfashion.catalog.dto.request.CreateProductDraftRequest;
import com.dzfashion.catalog.dto.request.CreateProductOwnerRequest;
import com.dzfashion.catalog.dto.request.CreateVariantDraftRequest;
import com.dzfashion.catalog.dto.request.CreateVariantRequest;
import com.dzfashion.catalog.dto.request.ProductFilterCriteria;
import com.dzfashion.catalog.dto.request.UpdateProductPriceRequest;
import com.dzfashion.catalog.dto.request.UpdateVariantPriceRequest;
import com.dzfashion.catalog.dto.response.MediaAssetResponse;
import com.dzfashion.catalog.dto.response.PageResponse;
import com.dzfashion.catalog.dto.response.ProductResponse;
import com.dzfashion.catalog.dto.response.ProductSummaryResponse;
import com.dzfashion.catalog.dto.response.ProductVariantResponse;
import com.dzfashion.catalog.entity.CategoryEntity;
import com.dzfashion.catalog.entity.MediaAssetEntity;
import com.dzfashion.catalog.entity.ProductEntity;
import com.dzfashion.catalog.entity.ProductVariantEntity;
import com.dzfashion.catalog.enums.ProductStatus;
import com.dzfashion.catalog.exception.InvalidOperationException;
import com.dzfashion.catalog.exception.ResourceConflictException;
import com.dzfashion.catalog.exception.ResourceNotFoundException;
import com.dzfashion.catalog.repository.CategoryRepository;
import com.dzfashion.catalog.repository.ProductRepository;
import com.dzfashion.catalog.repository.ProductVariantRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CategoryRepository categoryRepository;
    private final SkuGeneratorService skuGeneratorService;
    private final CloudinaryMediaService mediaService;

    public ProductService(ProductRepository productRepository,
                          ProductVariantRepository productVariantRepository,
                          CategoryRepository categoryRepository,
                          SkuGeneratorService skuGeneratorService,
                          CloudinaryMediaService mediaService) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.categoryRepository = categoryRepository;
        this.skuGeneratorService = skuGeneratorService;
        this.mediaService = mediaService;
    }

    @Transactional
    public ProductResponse createProductDraft(CreateProductDraftRequest request, UUID creatorId) {
        if (productRepository.existsBySlug(request.slug())) {
            throw new ResourceConflictException("Product slug already exists: " + request.slug());
        }

        CategoryEntity category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + request.categoryId()));

        ProductEntity product = new ProductEntity(
                category,
                request.name().trim(),
                request.slug().trim().toLowerCase(Locale.ROOT),
                request.description(),
                request.fabric(),
                request.occasion(),
                BigDecimal.ZERO,
                creatorId
        );
        product.setStatus(ProductStatus.DRAFT);

        if (request.variants() != null) {
            for (CreateVariantDraftRequest v : request.variants()) {
                String sku = skuGeneratorService.generateUniqueSku(
                        category.getName(), product.getSlug(), v.colour(), v.size()
                );
                ProductVariantEntity variant = new ProductVariantEntity(product, sku, v.size(), v.colour(), null);
                product.addVariant(variant);
            }
        }

        if (request.media() != null) {
            for (CreateMediaRequest m : request.media()) {
                MediaAssetEntity asset = new MediaAssetEntity(
                        product,
                        m.variantId(),
                        m.cloudinaryPublicId(),
                        m.secureUrl(),
                        m.format(),
                        m.width(),
                        m.height(),
                        m.altText(),
                        m.displayOrder(),
                        m.primary()
                );
                product.addMediaAsset(asset);
            }
        }

        ProductEntity saved = productRepository.save(product);
        return mapToResponse(saved);
    }

    @Transactional
    public ProductResponse createProductOwner(CreateProductOwnerRequest request, UUID ownerId) {
        if (productRepository.existsBySlug(request.slug())) {
            throw new ResourceConflictException("Product slug already exists: " + request.slug());
        }

        CategoryEntity category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + request.categoryId()));

        ProductEntity product = new ProductEntity(
                category,
                request.name().trim(),
                request.slug().trim().toLowerCase(Locale.ROOT),
                request.description(),
                request.fabric(),
                request.occasion(),
                request.defaultPrice(),
                ownerId
        );

        if (request.publishImmediately()) {
            product.setStatus(ProductStatus.PUBLISHED);
            product.setApprovedBy(ownerId);
        } else {
            product.setStatus(ProductStatus.DRAFT);
        }

        if (request.variants() != null) {
            for (CreateVariantRequest v : request.variants()) {
                String sku = skuGeneratorService.generateUniqueSku(
                        category.getName(), product.getSlug(), v.colour(), v.size()
                );
                ProductVariantEntity variant = new ProductVariantEntity(
                        product, sku, v.size(), v.colour(), v.priceOverride()
                );
                product.addVariant(variant);
            }
        }

        if (request.media() != null) {
            for (CreateMediaRequest m : request.media()) {
                MediaAssetEntity asset = new MediaAssetEntity(
                        product,
                        m.variantId(),
                        m.cloudinaryPublicId(),
                        m.secureUrl(),
                        m.format(),
                        m.width(),
                        m.height(),
                        m.altText(),
                        m.displayOrder(),
                        m.primary()
                );
                product.addMediaAsset(asset);
            }
        }

        ProductEntity saved = productRepository.save(product);
        return mapToResponse(saved);
    }

    @Transactional
    public ProductResponse updateProductPrice(UUID productId, UpdateProductPriceRequest request, UUID ownerId) {
        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));

        product.setDefaultPrice(request.defaultPrice());
        ProductEntity saved = productRepository.save(product);
        return mapToResponse(saved);
    }

    @Transactional
    public ProductVariantResponse updateVariantPrice(UUID productId, UUID variantId, UpdateVariantPriceRequest request, UUID ownerId) {
        ProductVariantEntity variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found with ID: " + variantId));

        if (!variant.getProduct().getId().equals(productId)) {
            throw new InvalidOperationException("Variant does not belong to product: " + productId);
        }

        variant.setPriceOverride(request.priceOverride());
        ProductVariantEntity saved = productVariantRepository.save(variant);
        return mapVariantToResponse(saved);
    }

    @Transactional
    public ProductResponse publishProduct(UUID productId, UUID ownerId) {
        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));

        if (product.getDefaultPrice() == null || product.getDefaultPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOperationException("Cannot publish product without a valid price set by the Owner");
        }

        product.setStatus(ProductStatus.PUBLISHED);
        product.setApprovedBy(ownerId);
        ProductEntity saved = productRepository.save(product);
        return mapToResponse(saved);
    }

    @Transactional
    public void archiveProduct(UUID productId, UUID ownerId) {
        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));

        product.setStatus(ProductStatus.ARCHIVED);
        productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(UUID id, boolean allowDrafts) {
        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

        if (!allowDrafts && product.getStatus() != ProductStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Published product not found with ID: " + id);
        }

        return mapToResponse(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductBySlug(String slug, boolean allowDrafts) {
        ProductEntity product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with slug: " + slug));

        if (!allowDrafts && product.getStatus() != ProductStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Published product not found with slug: " + slug);
        }

        return mapToResponse(product);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> searchAndFilterProducts(ProductFilterCriteria criteria, Pageable pageable) {
        Specification<ProductEntity> spec = createSpecification(criteria);
        Page<ProductEntity> page = productRepository.findAll(spec, pageable);

        List<ProductSummaryResponse> content = page.getContent().stream()
                .map(this::mapToSummary)
                .collect(Collectors.toList());

        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    @Transactional(readOnly = true)
    public List<ProductVariantResponse> listProductVariants(UUID productId, boolean activeOnly) {
        List<ProductVariantEntity> variants = activeOnly
                ? productVariantRepository.findByProductIdAndActiveTrue(productId)
                : productVariantRepository.findByProductId(productId);

        return variants.stream()
                .map(this::mapVariantToResponse)
                .collect(Collectors.toList());
    }

    private Specification<ProductEntity> createSpecification(ProductFilterCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Status filter (defaults to PUBLISHED if null)
            ProductStatus status = criteria.status() != null ? criteria.status() : ProductStatus.PUBLISHED;
            predicates.add(cb.equal(root.get("status"), status));

            // Category filters
            if (criteria.categoryId() != null) {
                predicates.add(cb.equal(root.get("category").get("id"), criteria.categoryId()));
            } else if (criteria.categorySlug() != null && !criteria.categorySlug().isBlank()) {
                predicates.add(cb.equal(root.get("category").get("slug"), criteria.categorySlug().toLowerCase(Locale.ROOT)));
            }

            // Price range filters
            if (criteria.minPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("defaultPrice"), criteria.minPrice()));
            }
            if (criteria.maxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("defaultPrice"), criteria.maxPrice()));
            }

            // Fabric filter
            if (criteria.fabric() != null && !criteria.fabric().isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("fabric")), criteria.fabric().toLowerCase(Locale.ROOT)));
            }

            // Occasion filter
            if (criteria.occasion() != null && !criteria.occasion().isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("occasion")), criteria.occasion().toLowerCase(Locale.ROOT)));
            }

            // Search filter across name, description, fabric, occasion
            if (criteria.search() != null && !criteria.search().isBlank()) {
                String searchPattern = "%" + criteria.search().toLowerCase(Locale.ROOT).trim() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("name")), searchPattern);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), searchPattern);
                Predicate fabricMatch = cb.like(cb.lower(root.get("fabric")), searchPattern);
                Predicate occasionMatch = cb.like(cb.lower(root.get("occasion")), searchPattern);
                predicates.add(cb.or(nameMatch, descMatch, fabricMatch, occasionMatch));
            }

            // Size or Colour filter (via join to variants)
            if ((criteria.size() != null && !criteria.size().isBlank()) ||
                    (criteria.colour() != null && !criteria.colour().isBlank())) {
                Join<ProductEntity, ProductVariantEntity> variantJoin = root.join("variants", JoinType.INNER);
                if (criteria.size() != null && !criteria.size().isBlank()) {
                    predicates.add(cb.equal(cb.lower(variantJoin.get("size")), criteria.size().toLowerCase(Locale.ROOT)));
                }
                if (criteria.colour() != null && !criteria.colour().isBlank()) {
                    predicates.add(cb.equal(cb.lower(variantJoin.get("colour")), criteria.colour().toLowerCase(Locale.ROOT)));
                }
                if (query != null) {
                    query.distinct(true);
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public ProductResponse mapToResponse(ProductEntity product) {
        String categoryName = product.getCategory() != null ? product.getCategory().getName() : "";
        String categorySlug = product.getCategory() != null ? product.getCategory().getSlug() : "";
        UUID categoryId = product.getCategory() != null ? product.getCategory().getId() : null;

        List<ProductVariantResponse> variantResponses = product.getVariants() != null
                ? product.getVariants().stream().map(this::mapVariantToResponse).collect(Collectors.toList())
                : List.of();

        List<MediaAssetResponse> mediaResponses = product.getMediaAssets() != null
                ? product.getMediaAssets().stream().map(mediaService::mapToResponse).collect(Collectors.toList())
                : List.of();

        return new ProductResponse(
                product.getId(),
                categoryId,
                categoryName,
                categorySlug,
                product.getName(),
                product.getSlug(),
                product.getDescription(),
                product.getFabric(),
                product.getOccasion(),
                product.getStatus(),
                product.getDefaultPrice(),
                variantResponses,
                mediaResponses,
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    public ProductSummaryResponse mapToSummary(ProductEntity product) {
        String categoryName = product.getCategory() != null ? product.getCategory().getName() : "";
        String primaryImageUrl = null;
        if (product.getMediaAssets() != null && !product.getMediaAssets().isEmpty()) {
            primaryImageUrl = product.getMediaAssets().stream()
                    .filter(MediaAssetEntity::isPrimary)
                    .map(MediaAssetEntity::getSecureUrl)
                    .findFirst()
                    .orElse(product.getMediaAssets().get(0).getSecureUrl());
        }

        return new ProductSummaryResponse(
                product.getId(),
                categoryName,
                product.getName(),
                product.getSlug(),
                product.getFabric(),
                product.getOccasion(),
                product.getDefaultPrice(),
                primaryImageUrl,
                product.getStatus()
        );
    }

    public ProductVariantResponse mapVariantToResponse(ProductVariantEntity variant) {
        return new ProductVariantResponse(
                variant.getId(),
                variant.getSku(),
                variant.getSize(),
                variant.getColour(),
                variant.getPriceOverride(),
                variant.getEffectivePrice(),
                variant.isActive()
        );
    }
}
