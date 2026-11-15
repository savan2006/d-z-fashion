package com.dzfashion.inventory.service;

import com.dzfashion.inventory.dto.response.LowStockAlertResponse;
import com.dzfashion.inventory.dto.response.PageResponse;
import com.dzfashion.inventory.entity.LowStockAlertEntity;
import com.dzfashion.inventory.entity.WarehouseStockEntity;
import com.dzfashion.inventory.repository.LowStockAlertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AlertNotificationService {

    private static final Logger log = LoggerFactory.getLogger(AlertNotificationService.class);

    private final LowStockAlertRepository alertRepository;

    public AlertNotificationService(LowStockAlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @Transactional
    public void evaluateStockLevel(WarehouseStockEntity stock) {
        int available = stock.getAvailableQty();
        int threshold = stock.getLowStockThreshold();

        if (available <= threshold) {
            // Trigger alert if not already active (suppress duplicate alerts)
            boolean alreadyAlerted = alertRepository.existsByVariantIdAndResolvedFalse(stock.getVariantId());
            if (!alreadyAlerted) {
                LowStockAlertEntity alert = new LowStockAlertEntity(
                        stock.getVariantId(),
                        stock.getSku(),
                        available,
                        threshold
                );
                alertRepository.save(alert);
                log.warn("LOW STOCK ALERT: Variant {} (SKU: {}) available stock {} <= threshold {}",
                        stock.getVariantId(), stock.getSku(), available, threshold);
            }
        } else {
            // If stock has recovered above threshold, auto-resolve any active alerts
            Optional<LowStockAlertEntity> activeAlert = alertRepository.findByVariantIdAndResolvedFalse(stock.getVariantId());
            activeAlert.ifPresent(alert -> {
                alert.setResolved(true);
                alert.setResolvedAt(Instant.now());
                alertRepository.save(alert);
                log.info("Low stock alert resolved for variant {} (SKU: {}). Available: {}",
                        stock.getVariantId(), stock.getSku(), available);
            });
        }
    }

    @Transactional(readOnly = true)
    public List<LowStockAlertResponse> getActiveAlerts() {
        return alertRepository.findByResolvedFalseOrderByAlertSentAtDesc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<LowStockAlertResponse> getActiveAlerts(Pageable pageable) {
        Page<LowStockAlertEntity> page = alertRepository.findByResolvedFalseOrderByAlertSentAtDesc(pageable);
        List<LowStockAlertResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
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

    private LowStockAlertResponse mapToResponse(LowStockAlertEntity entity) {
        return new LowStockAlertResponse(
                entity.getId(),
                entity.getVariantId(),
                entity.getSku(),
                entity.getAvailableQty(),
                entity.getThreshold(),
                entity.getAlertSentAt(),
                entity.isResolved()
        );
    }
}
