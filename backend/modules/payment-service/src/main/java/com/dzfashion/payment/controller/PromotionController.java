package com.dzfashion.payment.controller;

import com.dzfashion.payment.dto.request.ApplyPromotionRequest;
import com.dzfashion.payment.dto.request.CreatePromotionRequest;
import com.dzfashion.payment.dto.request.UpdatePromotionRequest;
import com.dzfashion.payment.dto.response.ActionResponse;
import com.dzfashion.payment.dto.response.PageResponse;
import com.dzfashion.payment.dto.response.PromotionEvaluationResponse;
import com.dzfashion.payment.dto.response.PromotionResponse;
import com.dzfashion.payment.security.UserPrincipal;
import com.dzfashion.payment.service.PromotionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class PromotionController {

    private final PromotionService promotionService;

    public PromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @PostMapping("/api/v1/promotions/evaluate")
    public ResponseEntity<PromotionEvaluationResponse> evaluatePromotion(
            @Valid @RequestBody ApplyPromotionRequest request) {
        PromotionEvaluationResponse response = promotionService.evaluatePromotion(request.couponCode(), request.subtotal());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/v1/admin/promotions")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<PromotionResponse> createPromotion(
            @Valid @RequestBody CreatePromotionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        PromotionResponse response = promotionService.createPromotion(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/api/v1/admin/promotions/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<PromotionResponse> updatePromotion(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePromotionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        PromotionResponse response = promotionService.updatePromotion(id, request, principal.getId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/api/v1/admin/promotions/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ActionResponse> deactivatePromotion(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        promotionService.deactivatePromotion(id, principal.getId());
        return ResponseEntity.ok(new ActionResponse(true, "Promotion deactivated successfully"));
    }

    @GetMapping("/api/v1/admin/promotions/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public ResponseEntity<PromotionResponse> getPromotionById(@PathVariable UUID id) {
        PromotionResponse response = promotionService.getPromotionById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/v1/admin/promotions")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public ResponseEntity<PageResponse<PromotionResponse>> getAllPromotions(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<PromotionResponse> page = promotionService.getAllPromotions(pageable);
        return ResponseEntity.ok(PageResponse.from(page));
    }
}
