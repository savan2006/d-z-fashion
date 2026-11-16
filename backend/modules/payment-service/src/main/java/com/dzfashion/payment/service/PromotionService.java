package com.dzfashion.payment.service;

import com.dzfashion.payment.dto.request.CreatePromotionRequest;
import com.dzfashion.payment.dto.request.UpdatePromotionRequest;
import com.dzfashion.payment.dto.response.PromotionEvaluationResponse;
import com.dzfashion.payment.dto.response.PromotionResponse;
import com.dzfashion.payment.entity.PromotionEntity;
import com.dzfashion.payment.enums.PromotionType;
import com.dzfashion.payment.exception.InvalidOperationException;
import com.dzfashion.payment.exception.ResourceConflictException;
import com.dzfashion.payment.exception.ResourceNotFoundException;
import com.dzfashion.payment.repository.PromotionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class PromotionService {

    private static final Logger log = LoggerFactory.getLogger(PromotionService.class);

    private final PromotionRepository promotionRepository;

    public PromotionService(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    public PromotionResponse createPromotion(CreatePromotionRequest request, UUID ownerId) {
        if (request.endsAt().isBefore(request.startsAt())) {
            throw new InvalidOperationException("Promotion end time must be after start time");
        }
        if (request.type() == PromotionType.PERCENTAGE && request.discountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new InvalidOperationException("Percentage discount cannot exceed 100%");
        }

        String normalizedCode;
        if (request.code() == null || request.code().isBlank()) {
            normalizedCode = "PROMO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        } else {
            normalizedCode = request.code().trim().toUpperCase(Locale.ROOT);
        }

        if (promotionRepository.findByCodeIgnoreCase(normalizedCode).isPresent()) {
            throw new ResourceConflictException("Promotion with code '" + normalizedCode + "' already exists");
        }

        PromotionEntity entity = new PromotionEntity();
        entity.setCode(normalizedCode);
        entity.setName(request.name().trim());
        entity.setType(request.type());
        entity.setDiscountValue(request.discountValue().setScale(2, RoundingMode.HALF_UP));
        entity.setMinOrderSubtotal(request.minOrderSubtotal() != null
                ? request.minOrderSubtotal().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        entity.setMaxDiscountAmount(request.maxDiscountAmount() != null
                ? request.maxDiscountAmount().setScale(2, RoundingMode.HALF_UP)
                : null);
        entity.setStartsAt(request.startsAt());
        entity.setEndsAt(request.endsAt());
        entity.setUsageLimit(request.usageLimit());
        entity.setUsageCount(0);
        entity.setActive(true);
        entity.setCreatedBy(ownerId);

        PromotionEntity saved = promotionRepository.save(entity);
        log.info("Created promotion {} (code: {}) by owner {}", saved.getId(), saved.getCode(), ownerId);
        return mapToResponse(saved);
    }

    public PromotionResponse updatePromotion(UUID promotionId, UpdatePromotionRequest request, UUID ownerId) {
        PromotionEntity entity = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found with ID: " + promotionId));

        if (request.endsAt().isBefore(request.startsAt())) {
            throw new InvalidOperationException("Promotion end time must be after start time");
        }
        if (request.type() == PromotionType.PERCENTAGE && request.discountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new InvalidOperationException("Percentage discount cannot exceed 100%");
        }

        entity.setName(request.name().trim());
        entity.setType(request.type());
        entity.setDiscountValue(request.discountValue().setScale(2, RoundingMode.HALF_UP));
        entity.setMinOrderSubtotal(request.minOrderSubtotal() != null
                ? request.minOrderSubtotal().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        entity.setMaxDiscountAmount(request.maxDiscountAmount() != null
                ? request.maxDiscountAmount().setScale(2, RoundingMode.HALF_UP)
                : null);
        entity.setStartsAt(request.startsAt());
        entity.setEndsAt(request.endsAt());
        entity.setUsageLimit(request.usageLimit());
        entity.setActive(request.active());

        PromotionEntity updated = promotionRepository.save(entity);
        log.info("Updated promotion {} (code: {}) by owner {}", updated.getId(), updated.getCode(), ownerId);
        return mapToResponse(updated);
    }

    public void deactivatePromotion(UUID promotionId, UUID ownerId) {
        PromotionEntity entity = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found with ID: " + promotionId));
        entity.setActive(false);
        promotionRepository.save(entity);
        log.info("Deactivated promotion {} by owner {}", promotionId, ownerId);
    }

    @Transactional(readOnly = true)
    public PromotionResponse getPromotionById(UUID promotionId) {
        PromotionEntity entity = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found with ID: " + promotionId));
        return mapToResponse(entity);
    }

    @Transactional(readOnly = true)
    public PromotionResponse getPromotionByCode(String code) {
        PromotionEntity entity = promotionRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found with code: " + code));
        return mapToResponse(entity);
    }

    @Transactional(readOnly = true)
    public Page<PromotionResponse> getAllPromotions(Pageable pageable) {
        return promotionRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public PromotionEvaluationResponse evaluatePromotion(String code, BigDecimal orderSubtotal) {
        if (code == null || code.isBlank()) {
            return PromotionEvaluationResponse.invalid("Promotion code is required");
        }

        var opt = promotionRepository.findByCodeIgnoreCase(code.trim());
        if (opt.isEmpty()) {
            return PromotionEvaluationResponse.invalid("Invalid promotion code: " + code);
        }

        PromotionEntity promotion = opt.get();

        if (!promotion.isActive()) {
            return PromotionEvaluationResponse.invalid("Promotion is no longer active");
        }

        Instant now = Instant.now();
        if (now.isBefore(promotion.getStartsAt())) {
            return PromotionEvaluationResponse.invalid("Promotion has not started yet");
        }
        if (now.isAfter(promotion.getEndsAt())) {
            return PromotionEvaluationResponse.invalid("Promotion has expired");
        }

        if (promotion.getUsageLimit() != null && promotion.getUsageCount() >= promotion.getUsageLimit()) {
            return PromotionEvaluationResponse.invalid("Promotion usage limit has been reached");
        }

        BigDecimal subtotal = orderSubtotal != null ? orderSubtotal : BigDecimal.ZERO;
        if (subtotal.compareTo(promotion.getMinOrderSubtotal()) < 0) {
            return PromotionEvaluationResponse.invalid(
                    String.format("Minimum order subtotal of ₹%.2f required to apply this promotion", promotion.getMinOrderSubtotal())
            );
        }

        BigDecimal discount;
        if (promotion.getType() == PromotionType.FIXED_AMOUNT) {
            discount = promotion.getDiscountValue().min(subtotal);
        } else {
            discount = subtotal.multiply(promotion.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (promotion.getMaxDiscountAmount() != null) {
                discount = discount.min(promotion.getMaxDiscountAmount());
            }
            discount = discount.min(subtotal);
        }

        return PromotionEvaluationResponse.applied(
                promotion.getId(),
                promotion.getCode(),
                discount.setScale(2, RoundingMode.HALF_UP)
        );
    }

    public void recordPromotionUsage(String code) {
        if (code == null || code.isBlank()) {
            return;
        }

        PromotionEntity promotion = promotionRepository.findByCodeWithLock(code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found with code: " + code));

        if (!promotion.isActive()) {
            throw new InvalidOperationException("Cannot use inactive promotion: " + code);
        }
        Instant now = Instant.now();
        if (now.isBefore(promotion.getStartsAt()) || now.isAfter(promotion.getEndsAt())) {
            throw new InvalidOperationException("Promotion is outside its valid window: " + code);
        }
        if (promotion.getUsageLimit() != null && promotion.getUsageCount() >= promotion.getUsageLimit()) {
            throw new InvalidOperationException("Promotion usage limit reached for code: " + code);
        }

        promotion.setUsageCount(promotion.getUsageCount() + 1);
        promotionRepository.save(promotion);
        log.info("Recorded usage for promotion code {} (new usage count: {})", promotion.getCode(), promotion.getUsageCount());
    }

    private PromotionResponse mapToResponse(PromotionEntity entity) {
        return new PromotionResponse(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getType(),
                entity.getDiscountValue(),
                entity.getMinOrderSubtotal(),
                entity.getMaxDiscountAmount(),
                entity.getStartsAt(),
                entity.getEndsAt(),
                entity.getUsageLimit(),
                entity.getUsageCount(),
                entity.isActive(),
                entity.getCreatedAt()
        );
    }
}
