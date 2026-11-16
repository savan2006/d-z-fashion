package com.dzfashion.catalog.controller;

import com.dzfashion.catalog.dto.request.ModerateReviewRequest;
import com.dzfashion.catalog.dto.response.PageResponse;
import com.dzfashion.catalog.dto.response.ProductReviewResponse;
import com.dzfashion.catalog.enums.ReviewStatus;
import com.dzfashion.catalog.security.UserPrincipal;
import com.dzfashion.catalog.service.ProductReviewService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/reviews")
public class AdminReviewController {

    private final ProductReviewService reviewService;

    public AdminReviewController(ProductReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<PageResponse<ProductReviewResponse>> getAllReviews(
            @RequestParam(required = false) ReviewStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<ProductReviewResponse> response = reviewService.getAllReviewsForAdmin(status, pageable);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ProductReviewResponse> moderateReview(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ModerateReviewRequest request) {
        ProductReviewResponse response = reviewService.moderateReview(id, request, principal.getId());
        return ResponseEntity.ok(response);
    }
}
