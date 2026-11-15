package com.dzfashion.catalog.controller;

import com.dzfashion.catalog.dto.request.CreateProductOwnerRequest;
import com.dzfashion.catalog.dto.request.ReviewRevisionRequest;
import com.dzfashion.catalog.dto.request.UpdateProductPriceRequest;
import com.dzfashion.catalog.dto.request.UpdateVariantPriceRequest;
import com.dzfashion.catalog.dto.response.ActionResponse;
import com.dzfashion.catalog.dto.response.ProductResponse;
import com.dzfashion.catalog.dto.response.ProductRevisionResponse;
import com.dzfashion.catalog.dto.response.ProductVariantResponse;
import com.dzfashion.catalog.security.UserPrincipal;
import com.dzfashion.catalog.service.ProductRevisionService;
import com.dzfashion.catalog.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/products")
public class ProductAdminController {

    private final ProductService productService;
    private final ProductRevisionService revisionService;

    public ProductAdminController(ProductService productService, ProductRevisionService revisionService) {
        this.productService = productService;
        this.revisionService = revisionService;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<ProductResponse> getAdminProductById(@PathVariable UUID id) {
        ProductResponse response = productService.getProductById(id, true);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ProductResponse> createProductByOwner(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateProductOwnerRequest request) {
        ProductResponse response = productService.createProductOwner(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}/price")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ProductResponse> updateProductPrice(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProductPriceRequest request) {
        ProductResponse response = productService.updateProductPrice(id, request, principal.getId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/variants/{variantId}/price")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ProductVariantResponse> updateVariantPrice(
            @PathVariable UUID id,
            @PathVariable UUID variantId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateVariantPriceRequest request) {
        ProductVariantResponse response = productService.updateVariantPrice(id, variantId, request, principal.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ProductResponse> publishProduct(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        ProductResponse response = productService.publishProduct(id, principal.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ActionResponse> archiveProduct(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        productService.archiveProduct(id, principal.getId());
        return ResponseEntity.ok(new ActionResponse("Product has been archived successfully"));
    }

    @GetMapping("/revisions/pending")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<List<ProductRevisionResponse>> getPendingRevisions() {
        List<ProductRevisionResponse> pending = revisionService.getPendingRevisions();
        return ResponseEntity.ok(pending);
    }

    @PostMapping("/revisions/{revisionId}/review")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ProductRevisionResponse> reviewRevision(
            @PathVariable UUID revisionId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody ReviewRevisionRequest request) {
        ProductRevisionResponse response = revisionService.reviewRevision(revisionId, principal.getId(), request);
        return ResponseEntity.ok(response);
    }
}
