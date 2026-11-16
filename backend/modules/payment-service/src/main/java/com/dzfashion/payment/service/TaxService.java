package com.dzfashion.payment.service;

import com.dzfashion.payment.dto.request.CreateTaxClassificationRequest;
import com.dzfashion.payment.dto.response.TaxClassificationResponse;
import com.dzfashion.payment.entity.TaxClassificationEntity;
import com.dzfashion.payment.exception.ResourceConflictException;
import com.dzfashion.payment.exception.ResourceNotFoundException;
import com.dzfashion.payment.exception.TaxConfigurationException;
import com.dzfashion.payment.repository.TaxClassificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TaxService {

    private static final Logger log = LoggerFactory.getLogger(TaxService.class);

    private final TaxClassificationRepository taxClassificationRepository;

    public TaxService(TaxClassificationRepository taxClassificationRepository) {
        this.taxClassificationRepository = taxClassificationRepository;
    }

    public TaxClassificationResponse createTaxClassification(CreateTaxClassificationRequest request) {
        String hsn = request.hsnCode().trim();
        if (taxClassificationRepository.findByHsnCode(hsn).isPresent()) {
            throw new ResourceConflictException("Tax classification for HSN code '" + hsn + "' already exists");
        }

        TaxClassificationEntity entity = new TaxClassificationEntity(
                hsn,
                request.description().trim(),
                request.gstRatePercent().setScale(2, RoundingMode.HALF_UP),
                true
        );

        TaxClassificationEntity saved = taxClassificationRepository.save(entity);
        log.info("Created tax classification HSN {} with rate {}%", saved.getHsnCode(), saved.getGstRatePercent());
        return mapToResponse(saved);
    }

    public TaxClassificationResponse updateTaxClassification(UUID id, CreateTaxClassificationRequest request) {
        TaxClassificationEntity entity = taxClassificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tax classification not found with ID: " + id));

        entity.setDescription(request.description().trim());
        entity.setGstRatePercent(request.gstRatePercent().setScale(2, RoundingMode.HALF_UP));

        TaxClassificationEntity saved = taxClassificationRepository.save(entity);
        log.info("Updated tax classification ID {} (HSN: {})", saved.getId(), saved.getHsnCode());
        return mapToResponse(saved);
    }

    public void deactivateTaxClassification(UUID id) {
        TaxClassificationEntity entity = taxClassificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tax classification not found with ID: " + id));
        entity.setActive(false);
        taxClassificationRepository.save(entity);
        log.info("Deactivated tax classification ID {} (HSN: {})", entity.getId(), entity.getHsnCode());
    }

    @Transactional(readOnly = true)
    public List<TaxClassificationResponse> getAllTaxClassifications() {
        return taxClassificationRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TaxClassificationResponse> getActiveTaxClassifications() {
        return taxClassificationRepository.findByActiveTrue().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TaxClassificationEntity getActiveClassificationByHsn(String hsnCode) {
        if (hsnCode == null || hsnCode.isBlank()) {
            throw new TaxConfigurationException("Missing HSN code for tax calculation");
        }
        return taxClassificationRepository.findByHsnCode(hsnCode.trim())
                .filter(TaxClassificationEntity::isActive)
                .orElseThrow(() -> new TaxConfigurationException("Unknown or inactive tax configuration for HSN code: " + hsnCode));
    }

    private TaxClassificationResponse mapToResponse(TaxClassificationEntity entity) {
        return new TaxClassificationResponse(
                entity.getId(),
                entity.getHsnCode(),
                entity.getDescription(),
                entity.getGstRatePercent(),
                entity.isActive()
        );
    }
}
