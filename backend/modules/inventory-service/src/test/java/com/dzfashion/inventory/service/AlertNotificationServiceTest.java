package com.dzfashion.inventory.service;

import com.dzfashion.inventory.dto.response.LowStockAlertResponse;
import com.dzfashion.inventory.entity.LowStockAlertEntity;
import com.dzfashion.inventory.entity.WarehouseStockEntity;
import com.dzfashion.inventory.repository.LowStockAlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertNotificationServiceTest {

    @Mock
    private LowStockAlertRepository alertRepository;

    @InjectMocks
    private AlertNotificationService alertService;

    private UUID variantId;
    private WarehouseStockEntity stockEntity;

    @BeforeEach
    void setUp() {
        variantId = UUID.randomUUID();
        stockEntity = new WarehouseStockEntity(null, variantId, "DZF-KUR-TEST-001", 3, 5); // Available = 3, Threshold = 5
        stockEntity.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Creates low stock alert when available stock is below or equal to threshold")
    void evaluateStockLevelCreatesAlert() {
        when(alertRepository.existsByVariantIdAndResolvedFalse(variantId)).thenReturn(false);

        alertService.evaluateStockLevel(stockEntity);

        verify(alertRepository).save(any(LowStockAlertEntity.class));
    }

    @Test
    @DisplayName("Suppresses duplicate alert when an unresolved alert is already active")
    void evaluateStockLevelSuppressesDuplicateAlert() {
        when(alertRepository.existsByVariantIdAndResolvedFalse(variantId)).thenReturn(true);

        alertService.evaluateStockLevel(stockEntity);

        verify(alertRepository, never()).save(any(LowStockAlertEntity.class));
    }

    @Test
    @DisplayName("Auto-resolves active alert when stock level recovers above threshold")
    void evaluateStockLevelAutoResolvesAlert() {
        stockEntity.setPhysicalQty(20); // Available = 20 > Threshold 5
        LowStockAlertEntity activeAlert = new LowStockAlertEntity(variantId, "DZF-KUR-TEST-001", 3, 5);

        when(alertRepository.findByVariantIdAndResolvedFalse(variantId)).thenReturn(Optional.of(activeAlert));

        alertService.evaluateStockLevel(stockEntity);

        assertThat(activeAlert.isResolved()).isTrue();
        assertThat(activeAlert.getResolvedAt()).isNotNull();
        verify(alertRepository).save(activeAlert);
    }

    @Test
    @DisplayName("Retrieves active unresolved alerts")
    void getActiveAlertsReturnsList() {
        LowStockAlertEntity alert = new LowStockAlertEntity(variantId, "DZF-KUR-TEST-001", 2, 5);
        alert.setId(UUID.randomUUID());

        when(alertRepository.findByResolvedFalseOrderByAlertSentAtDesc()).thenReturn(List.of(alert));

        List<LowStockAlertResponse> alerts = alertService.getActiveAlerts();

        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).sku()).isEqualTo("DZF-KUR-TEST-001");
        assertThat(alerts.get(0).availableQty()).isEqualTo(2);
    }
}
