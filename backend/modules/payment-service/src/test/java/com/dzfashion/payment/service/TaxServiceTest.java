package com.dzfashion.payment.service;

import com.dzfashion.payment.dto.request.CreateTaxClassificationRequest;
import com.dzfashion.payment.dto.response.TaxClassificationResponse;
import com.dzfashion.payment.entity.TaxClassificationEntity;
import com.dzfashion.payment.exception.ResourceConflictException;
import com.dzfashion.payment.exception.TaxConfigurationException;
import com.dzfashion.payment.repository.TaxClassificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaxServiceTest {

    @Mock
    private TaxClassificationRepository taxClassificationRepository;

    private TaxService service;

    @BeforeEach
    void setUp() {
        service = new TaxService(taxClassificationRepository);
    }

    @Test
    @DisplayName("createTaxClassification creates active classification")
    void createTaxClassification_Success() {
        CreateTaxClassificationRequest request = new CreateTaxClassificationRequest(
                "6204", "Women's suits, dresses, skirts", new BigDecimal("5.00"));

        when(taxClassificationRepository.findByHsnCode("6204")).thenReturn(Optional.empty());
        when(taxClassificationRepository.save(any(TaxClassificationEntity.class))).thenAnswer(inv -> {
            TaxClassificationEntity e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        TaxClassificationResponse response = service.createTaxClassification(request);

        assertThat(response.hsnCode()).isEqualTo("6204");
        assertThat(response.gstRatePercent()).isEqualByComparingTo("5.00");
        assertThat(response.active()).isTrue();
    }

    @Test
    @DisplayName("createTaxClassification with duplicate HSN code throws ResourceConflictException")
    void createTaxClassification_Duplicate_ThrowsConflict() {
        CreateTaxClassificationRequest request = new CreateTaxClassificationRequest(
                "6204", "Duplicate entry", new BigDecimal("5.00"));

        when(taxClassificationRepository.findByHsnCode("6204"))
                .thenReturn(Optional.of(new TaxClassificationEntity()));

        assertThatThrownBy(() -> service.createTaxClassification(request))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("getActiveClassificationByHsn returns active classification")
    void getActiveClassificationByHsn_Success() {
        TaxClassificationEntity entity = new TaxClassificationEntity(
                "6204", "Women's clothing", new BigDecimal("5.00"), true);
        when(taxClassificationRepository.findByHsnCode("6204")).thenReturn(Optional.of(entity));

        TaxClassificationEntity found = service.getActiveClassificationByHsn("6204");
        assertThat(found.getHsnCode()).isEqualTo("6204");
    }

    @Test
    @DisplayName("getActiveClassificationByHsn with unknown HSN throws domain TaxConfigurationException")
    void getActiveClassificationByHsn_NotFound_ThrowsTaxConfigurationException() {
        when(taxClassificationRepository.findByHsnCode("9999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getActiveClassificationByHsn("9999"))
                .isInstanceOf(TaxConfigurationException.class)
                .hasMessageContaining("Unknown or inactive tax configuration for HSN code: 9999");
    }

    @Test
    @DisplayName("getActiveClassificationByHsn with inactive HSN throws TaxConfigurationException")
    void getActiveClassificationByHsn_Inactive_ThrowsTaxConfigurationException() {
        TaxClassificationEntity inactive = new TaxClassificationEntity(
                "6204", "Women's clothing", new BigDecimal("5.00"), false);
        when(taxClassificationRepository.findByHsnCode("6204")).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> service.getActiveClassificationByHsn("6204"))
                .isInstanceOf(TaxConfigurationException.class)
                .hasMessageContaining("Unknown or inactive tax configuration for HSN code: 6204");
    }
}
