package com.dzfashion.payment.service;

import com.dzfashion.payment.dto.request.CreatePromotionRequest;
import com.dzfashion.payment.dto.request.UpdatePromotionRequest;
import com.dzfashion.payment.dto.response.PromotionEvaluationResponse;
import com.dzfashion.payment.dto.response.PromotionResponse;
import com.dzfashion.payment.entity.PromotionEntity;
import com.dzfashion.payment.enums.PromotionType;
import com.dzfashion.payment.exception.InvalidOperationException;
import com.dzfashion.payment.exception.ResourceConflictException;
import com.dzfashion.payment.repository.PromotionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromotionServiceTest {

    @Mock
    private PromotionRepository promotionRepository;

    private PromotionService service;

    @BeforeEach
    void setUp() {
        service = new PromotionService(promotionRepository);
    }

    @Test
    @DisplayName("createPromotion creates valid percentage promotion")
    void createPromotion_ValidPercentage_Success() {
        UUID ownerId = UUID.randomUUID();
        CreatePromotionRequest request = new CreatePromotionRequest(
                "FESTIVE20",
                "Festive 20% Off",
                PromotionType.PERCENTAGE,
                new BigDecimal("20.00"),
                new BigDecimal("1000.00"),
                new BigDecimal("500.00"),
                Instant.now().minus(1, ChronoUnit.DAYS),
                Instant.now().plus(10, ChronoUnit.DAYS),
                100
        );

        when(promotionRepository.findByCodeIgnoreCase("FESTIVE20")).thenReturn(Optional.empty());
        when(promotionRepository.save(any(PromotionEntity.class))).thenAnswer(inv -> {
            PromotionEntity e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        PromotionResponse response = service.createPromotion(request, ownerId);

        assertThat(response.code()).isEqualTo("FESTIVE20");
        assertThat(response.discountValue()).isEqualByComparingTo("20.00");
        assertThat(response.maxDiscountAmount()).isEqualByComparingTo("500.00");
        assertThat(response.active()).isTrue();
    }

    @Test
    @DisplayName("createPromotion with percentage exceeding 100 throws InvalidOperationException")
    void createPromotion_PercentageOver100_ThrowsException() {
        CreatePromotionRequest request = new CreatePromotionRequest(
                "OVER100",
                "Excessive Discount",
                PromotionType.PERCENTAGE,
                new BigDecimal("105.00"),
                BigDecimal.ZERO,
                null,
                Instant.now(),
                Instant.now().plus(5, ChronoUnit.DAYS),
                null
        );

        assertThatThrownBy(() -> service.createPromotion(request, UUID.randomUUID()))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Percentage discount cannot exceed 100%");
    }

    @Test
    @DisplayName("createPromotion with end time before start time throws InvalidOperationException")
    void createPromotion_EndBeforeStart_ThrowsException() {
        CreatePromotionRequest request = new CreatePromotionRequest(
                "BADTIME",
                "Bad Timeline",
                PromotionType.FIXED_AMOUNT,
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                null,
                Instant.now().plus(5, ChronoUnit.DAYS),
                Instant.now().minus(1, ChronoUnit.DAYS),
                null
        );

        assertThatThrownBy(() -> service.createPromotion(request, UUID.randomUUID()))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Promotion end time must be after start time");
    }

    @Test
    @DisplayName("createPromotion with duplicate code throws ResourceConflictException")
    void createPromotion_DuplicateCode_ThrowsException() {
        CreatePromotionRequest request = new CreatePromotionRequest(
                "DUPLICATE",
                "Existing",
                PromotionType.FIXED_AMOUNT,
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                null,
                Instant.now(),
                Instant.now().plus(5, ChronoUnit.DAYS),
                null
        );

        when(promotionRepository.findByCodeIgnoreCase("DUPLICATE"))
                .thenReturn(Optional.of(new PromotionEntity()));

        assertThatThrownBy(() -> service.createPromotion(request, UUID.randomUUID()))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("evaluatePromotion calculates capped percentage discount accurately")
    void evaluatePromotion_PercentageWithCap_CalculatesCapped() {
        PromotionEntity entity = new PromotionEntity();
        entity.setId(UUID.randomUUID());
        entity.setCode("FESTIVE20");
        entity.setType(PromotionType.PERCENTAGE);
        entity.setDiscountValue(new BigDecimal("20.00"));
        entity.setMinOrderSubtotal(new BigDecimal("1000.00"));
        entity.setMaxDiscountAmount(new BigDecimal("500.00"));
        entity.setStartsAt(Instant.now().minus(2, ChronoUnit.DAYS));
        entity.setEndsAt(Instant.now().plus(5, ChronoUnit.DAYS));
        entity.setUsageLimit(50);
        entity.setUsageCount(10);
        entity.setActive(true);

        when(promotionRepository.findByCodeIgnoreCase("FESTIVE20")).thenReturn(Optional.of(entity));

        // Subtotal = 5000 -> 20% of 5000 is 1000, but cap is 500 -> discount = 500
        PromotionEvaluationResponse response = service.evaluatePromotion("FESTIVE20", new BigDecimal("5000.00"));

        assertThat(response.valid()).isTrue();
        assertThat(response.discountAmount()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("evaluatePromotion below minimum order subtotal returns invalid")
    void evaluatePromotion_BelowMinSubtotal_ReturnsInvalid() {
        PromotionEntity entity = new PromotionEntity();
        entity.setCode("MINORDER");
        entity.setType(PromotionType.FIXED_AMOUNT);
        entity.setDiscountValue(new BigDecimal("150.00"));
        entity.setMinOrderSubtotal(new BigDecimal("2000.00"));
        entity.setStartsAt(Instant.now().minus(1, ChronoUnit.DAYS));
        entity.setEndsAt(Instant.now().plus(5, ChronoUnit.DAYS));
        entity.setActive(true);

        when(promotionRepository.findByCodeIgnoreCase("MINORDER")).thenReturn(Optional.of(entity));

        PromotionEvaluationResponse response = service.evaluatePromotion("MINORDER", new BigDecimal("1500.00"));

        assertThat(response.valid()).isFalse();
        assertThat(response.discountAmount()).isEqualByComparingTo("0.00");
        assertThat(response.message()).contains("Minimum order subtotal of ₹2000.00 required");
    }

    @Test
    @DisplayName("evaluatePromotion outside active date window returns invalid")
    void evaluatePromotion_Expired_ReturnsInvalid() {
        PromotionEntity entity = new PromotionEntity();
        entity.setCode("EXPIRED");
        entity.setType(PromotionType.FIXED_AMOUNT);
        entity.setDiscountValue(new BigDecimal("100.00"));
        entity.setMinOrderSubtotal(BigDecimal.ZERO);
        entity.setStartsAt(Instant.now().minus(10, ChronoUnit.DAYS));
        entity.setEndsAt(Instant.now().minus(1, ChronoUnit.DAYS));
        entity.setActive(true);

        when(promotionRepository.findByCodeIgnoreCase("EXPIRED")).thenReturn(Optional.of(entity));

        PromotionEvaluationResponse response = service.evaluatePromotion("EXPIRED", new BigDecimal("1000.00"));

        assertThat(response.valid()).isFalse();
        assertThat(response.message()).contains("expired");
    }

    @Test
    @DisplayName("evaluatePromotion exceeding usage limit returns invalid")
    void evaluatePromotion_UsageLimitReached_ReturnsInvalid() {
        PromotionEntity entity = new PromotionEntity();
        entity.setCode("LIMITED");
        entity.setType(PromotionType.FIXED_AMOUNT);
        entity.setDiscountValue(new BigDecimal("100.00"));
        entity.setMinOrderSubtotal(BigDecimal.ZERO);
        entity.setStartsAt(Instant.now().minus(1, ChronoUnit.DAYS));
        entity.setEndsAt(Instant.now().plus(5, ChronoUnit.DAYS));
        entity.setUsageLimit(10);
        entity.setUsageCount(10);
        entity.setActive(true);

        when(promotionRepository.findByCodeIgnoreCase("LIMITED")).thenReturn(Optional.of(entity));

        PromotionEvaluationResponse response = service.evaluatePromotion("LIMITED", new BigDecimal("1000.00"));

        assertThat(response.valid()).isFalse();
        assertThat(response.message()).contains("usage limit has been reached");
    }

    @Test
    @DisplayName("recordPromotionUsage increments usage count atomically")
    void recordPromotionUsage_Success() {
        PromotionEntity entity = new PromotionEntity();
        entity.setCode("USEME");
        entity.setStartsAt(Instant.now().minus(1, ChronoUnit.DAYS));
        entity.setEndsAt(Instant.now().plus(5, ChronoUnit.DAYS));
        entity.setUsageLimit(5);
        entity.setUsageCount(2);
        entity.setActive(true);

        when(promotionRepository.findByCodeWithLock("USEME")).thenReturn(Optional.of(entity));
        when(promotionRepository.save(any(PromotionEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        service.recordPromotionUsage("USEME");

        assertThat(entity.getUsageCount()).isEqualTo(3);
    }
}
