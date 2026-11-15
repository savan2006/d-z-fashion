package com.dzfashion.catalog.service;

import com.dzfashion.catalog.dto.request.CreateVariantDraftRequest;
import com.dzfashion.catalog.dto.request.ReviewRevisionRequest;
import com.dzfashion.catalog.dto.request.SubmitRevisionRequest;
import com.dzfashion.catalog.dto.response.ProductRevisionResponse;
import com.dzfashion.catalog.entity.CategoryEntity;
import com.dzfashion.catalog.entity.ProductEntity;
import com.dzfashion.catalog.entity.ProductRevisionEntity;
import com.dzfashion.catalog.entity.ProductVariantEntity;
import com.dzfashion.catalog.enums.RevisionStatus;
import com.dzfashion.catalog.exception.InvalidOperationException;
import com.dzfashion.catalog.exception.ResourceNotFoundException;
import com.dzfashion.catalog.repository.CategoryRepository;
import com.dzfashion.catalog.repository.ProductRepository;
import com.dzfashion.catalog.repository.ProductRevisionRepository;
import com.dzfashion.catalog.repository.ProductVariantRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProductRevisionService {

    private final ProductRevisionRepository productRevisionRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CategoryRepository categoryRepository;
    private final SkuGeneratorService skuGeneratorService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProductRevisionService(ProductRevisionRepository productRevisionRepository,
                                  ProductRepository productRepository,
                                  ProductVariantRepository productVariantRepository,
                                  CategoryRepository categoryRepository,
                                  SkuGeneratorService skuGeneratorService) {
        this.productRevisionRepository = productRevisionRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.categoryRepository = categoryRepository;
        this.skuGeneratorService = skuGeneratorService;
    }

    @Transactional
    public ProductRevisionResponse submitRevision(UUID productId, UUID managerId, SubmitRevisionRequest request) {
        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));

        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new InvalidOperationException("Failed to serialize revision payload: " + e.getMessage());
        }

        ProductRevisionEntity revision = new ProductRevisionEntity(product, managerId, jsonPayload);
        ProductRevisionEntity saved = productRevisionRepository.save(revision);

        return mapToResponse(saved);
    }

    @Transactional
    public ProductRevisionResponse reviewRevision(UUID revisionId, UUID ownerId, ReviewRevisionRequest reviewRequest) {
        ProductRevisionEntity revision = productRevisionRepository.findById(revisionId)
                .orElseThrow(() -> new ResourceNotFoundException("Product revision not found with ID: " + revisionId));

        if (revision.getStatus() != RevisionStatus.PENDING) {
            throw new InvalidOperationException("Revision has already been processed with status: " + revision.getStatus());
        }

        revision.setReviewedBy(ownerId);
        revision.setReviewedAt(Instant.now());

        if (reviewRequest.approved()) {
            ProductEntity product = revision.getProduct();
            applyApprovedChanges(product, revision.getProposedChanges());
            productRepository.save(product);
            revision.setStatus(RevisionStatus.APPROVED);
        } else {
            revision.setStatus(RevisionStatus.REJECTED);
            revision.setRejectionReason(reviewRequest.rejectionReason());
        }

        ProductRevisionEntity updated = productRevisionRepository.save(revision);
        return mapToResponse(updated);
    }

    private void applyApprovedChanges(ProductEntity product, String jsonPayload) {
        try {
            SubmitRevisionRequest changes = objectMapper.readValue(jsonPayload, SubmitRevisionRequest.class);

            if (changes.name() != null && !changes.name().isBlank()) {
                product.setName(changes.name().trim());
            }
            if (changes.description() != null) {
                product.setDescription(changes.description());
            }
            if (changes.fabric() != null) {
                product.setFabric(changes.fabric().trim());
            }
            if (changes.occasion() != null) {
                product.setOccasion(changes.occasion().trim());
            }
            if (changes.categoryId() != null) {
                CategoryEntity newCategory = categoryRepository.findById(changes.categoryId())
                        .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + changes.categoryId()));
                product.setCategory(newCategory);
            }

            if (changes.addedVariants() != null && !changes.addedVariants().isEmpty()) {
                for (CreateVariantDraftRequest v : changes.addedVariants()) {
                    boolean exists = productVariantRepository.existsByProductIdAndSizeAndColour(
                            product.getId(), v.size(), v.colour()
                    );
                    if (!exists) {
                        String sku = skuGeneratorService.generateUniqueSku(
                                product.getCategory().getName(),
                                product.getSlug(),
                                v.colour(),
                                v.size()
                        );
                        ProductVariantEntity variant = new ProductVariantEntity(product, sku, v.size(), v.colour(), null);
                        product.addVariant(variant);
                    }
                }
            }
        } catch (JsonProcessingException e) {
            throw new InvalidOperationException("Failed to parse approved revision payload: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<ProductRevisionResponse> getPendingRevisions() {
        return productRevisionRepository.findByStatusOrderByCreatedAtDesc(RevisionStatus.PENDING).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductRevisionResponse> getProductRevisions(UUID productId) {
        return productRevisionRepository.findByProductIdOrderByCreatedAtDesc(productId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductRevisionResponse getRevisionById(UUID revisionId) {
        ProductRevisionEntity revision = productRevisionRepository.findById(revisionId)
                .orElseThrow(() -> new ResourceNotFoundException("Product revision not found with ID: " + revisionId));
        return mapToResponse(revision);
    }

    private ProductRevisionResponse mapToResponse(ProductRevisionEntity entity) {
        String productName = entity.getProduct() != null ? entity.getProduct().getName() : "Unknown";
        return new ProductRevisionResponse(
                entity.getId(),
                entity.getProduct().getId(),
                productName,
                entity.getSubmittedBy(),
                entity.getReviewedBy(),
                entity.getProposedChanges(),
                entity.getStatus(),
                entity.getRejectionReason(),
                entity.getCreatedAt(),
                entity.getReviewedAt()
        );
    }
}
