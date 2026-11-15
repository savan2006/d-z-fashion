package com.dzfashion.catalog.controller;

import com.dzfashion.catalog.dto.request.CreateProductDraftRequest;
import com.dzfashion.catalog.dto.request.ProductFilterCriteria;
import com.dzfashion.catalog.dto.request.SubmitRevisionRequest;
import com.dzfashion.catalog.dto.response.PageResponse;
import com.dzfashion.catalog.dto.response.ProductResponse;
import com.dzfashion.catalog.dto.response.ProductRevisionResponse;
import com.dzfashion.catalog.dto.response.ProductSummaryResponse;
import com.dzfashion.catalog.dto.response.ProductVariantResponse;
import com.dzfashion.catalog.enums.ProductStatus;
import com.dzfashion.catalog.security.UserPrincipal;
import com.dzfashion.catalog.service.ProductRevisionService;
import com.dzfashion.catalog.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;
    private final ProductRevisionService revisionService;

    public ProductController(ProductService productService, ProductRevisionService revisionService) {
        this.productService = productService;
        this.revisionService = revisionService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<ProductSummaryResponse>> listProducts(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String categorySlug,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) String colour,
            @RequestParam(required = false) String fabric,
            @RequestParam(required = false) String occasion,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(direction, sortField));

        ProductFilterCriteria criteria = new ProductFilterCriteria(
                categoryId, categorySlug, minPrice, maxPrice, size, colour, fabric, occasion, search, ProductStatus.PUBLISHED
        );

        PageResponse<ProductSummaryResponse> response = productService.searchAndFilterProducts(criteria, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable UUID id) {
        ProductResponse response = productService.getProductById(id, false);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<ProductResponse> getProductBySlug(@PathVariable String slug) {
        ProductResponse response = productService.getProductBySlug(slug, false);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/variants")
    public ResponseEntity<List<ProductVariantResponse>> listVariants(@PathVariable UUID id) {
        List<ProductVariantResponse> variants = productService.listProductVariants(id, true);
        return ResponseEntity.ok(variants);
    }

    @PostMapping("/draft")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<ProductResponse> createDraft(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateProductDraftRequest request) {
        ProductResponse response = productService.createProductDraft(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/revisions")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<ProductRevisionResponse> submitRevision(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SubmitRevisionRequest request) {
        ProductRevisionResponse response = revisionService.submitRevision(id, principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/revisions")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<List<ProductRevisionResponse>> getProductRevisions(@PathVariable UUID id) {
        List<ProductRevisionResponse> revisions = revisionService.getProductRevisions(id);
        return ResponseEntity.ok(revisions);
    }
}
