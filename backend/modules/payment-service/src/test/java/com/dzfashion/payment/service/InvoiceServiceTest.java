package com.dzfashion.payment.service;

import com.dzfashion.payment.config.PaymentProperties;
import com.dzfashion.payment.dto.request.GenerateDispatchInvoiceRequest;
import com.dzfashion.payment.dto.request.GenerateReceiptRequest;
import com.dzfashion.payment.dto.request.InvoiceItemData;
import com.dzfashion.payment.dto.response.InvoiceResponse;
import com.dzfashion.payment.entity.InvoiceEntity;
import com.dzfashion.payment.entity.InvoiceSequenceEntity;
import com.dzfashion.payment.entity.OutboxEventEntity;
import com.dzfashion.payment.entity.TaxClassificationEntity;
import com.dzfashion.payment.enums.InvoiceType;
import com.dzfashion.payment.exception.TaxConfigurationException;
import com.dzfashion.payment.repository.InvoiceRepository;
import com.dzfashion.payment.repository.InvoiceSequenceRepository;
import com.dzfashion.payment.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private InvoiceSequenceRepository invoiceSequenceRepository;

    @Mock
    private TaxService taxService;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private PaymentProperties paymentProperties;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private InvoiceService service;

    @BeforeEach
    void setUp() {
        paymentProperties = new PaymentProperties();
        paymentProperties.setSellerLegalName("D & Z Fashion");
        paymentProperties.setSellerGstin("24AAACD1234E1Z5");
        paymentProperties.setSellerAddress("Surat Central Warehouse, Ring Road, Surat, Gujarat - 395002");
        paymentProperties.setSellerState("Gujarat");

        service = new InvoiceService(invoiceRepository, invoiceSequenceRepository, taxService,
                outboxEventRepository, paymentProperties, objectMapper);
    }

    @Test
    @DisplayName("generateReceipt generates RCP sequence invoice upon order placement")
    void generateReceipt_Success() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        GenerateReceiptRequest request = new GenerateReceiptRequest(
                orderId,
                "ORD-20261116-1001",
                customerId,
                "Aarav Patel",
                "9876543210",
                "101 Green Avenue, Ahmedabad, Gujarat",
                "Gujarat",
                new BigDecimal("2000.00"),
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("2100.00"),
                List.of(new InvoiceItemData(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Anarkali Kurta",
                        "KURTA-RED-M",
                        "6204",
                        1,
                        new BigDecimal("2000.00"),
                        new BigDecimal("2000.00"),
                        new BigDecimal("5.00")
                ))
        );

        when(invoiceRepository.findByOrderIdAndInvoiceType(orderId, InvoiceType.RECEIPT))
                .thenReturn(Optional.empty());

        InvoiceSequenceEntity seq = new InvoiceSequenceEntity("2026-27");
        when(invoiceSequenceRepository.findByFiscalYearWithLock(any())).thenReturn(Optional.of(seq));
        when(invoiceRepository.save(any(InvoiceEntity.class))).thenAnswer(inv -> {
            InvoiceEntity e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        InvoiceResponse response = service.generateReceipt(request);

        assertThat(response.invoiceType()).isEqualTo(InvoiceType.RECEIPT);
        assertThat(response.invoiceNumber()).startsWith("RCP/");
        assertThat(response.items()).hasSize(1);
        verify(outboxEventRepository).save(any(OutboxEventEntity.class));
    }

    @Test
    @DisplayName("generateDispatchInvoice splits tax equally into CGST and SGST for intra-state Gujarat supply")
    void generateDispatchInvoice_IntraStateGujarat_SplitsCgstSgst() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        GenerateDispatchInvoiceRequest request = new GenerateDispatchInvoiceRequest(
                orderId,
                "ORD-20261116-1002",
                customerId,
                "Pooja Shah",
                "9876543211",
                "202 Ring Road, Surat, Gujarat - 395002",
                "Gujarat", // Intra-state with Surat, Gujarat
                new BigDecimal("2000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("2100.00"),
                List.of(new InvoiceItemData(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Silk Saree",
                        "SAREE-SLK-BL",
                        "6204",
                        1,
                        new BigDecimal("2000.00"),
                        new BigDecimal("2000.00"),
                        new BigDecimal("5.00")
                ))
        );

        when(invoiceRepository.findByOrderIdAndInvoiceType(orderId, InvoiceType.DISPATCH_TAX_INVOICE))
                .thenReturn(Optional.empty());

        TaxClassificationEntity taxEntity = new TaxClassificationEntity(
                "6204", "Women's apparel", new BigDecimal("5.00"), true);
        when(taxService.getActiveClassificationByHsn("6204")).thenReturn(taxEntity);

        InvoiceSequenceEntity seq = new InvoiceSequenceEntity("2026-27");
        when(invoiceSequenceRepository.findByFiscalYearWithLock(any())).thenReturn(Optional.of(seq));
        when(invoiceRepository.save(any(InvoiceEntity.class))).thenAnswer(inv -> {
            InvoiceEntity e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        InvoiceResponse response = service.generateDispatchInvoice(request);

        assertThat(response.invoiceType()).isEqualTo(InvoiceType.DISPATCH_TAX_INVOICE);
        assertThat(response.invoiceNumber()).startsWith("INV/");
        // 5% of 2000 is 100 -> CGST: 50, SGST: 50, IGST: 0
        assertThat(response.cgstAmount()).isEqualByComparingTo("50.00");
        assertThat(response.sgstAmount()).isEqualByComparingTo("50.00");
        assertThat(response.igstAmount()).isEqualByComparingTo("0.00");
        assertThat(response.totalTaxAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("generateDispatchInvoice applies full IGST for inter-state Maharashtra supply")
    void generateDispatchInvoice_InterStateMaharashtra_AppliesIgst() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        GenerateDispatchInvoiceRequest request = new GenerateDispatchInvoiceRequest(
                orderId,
                "ORD-20261116-1003",
                customerId,
                "Rohit Sharma",
                "9876543212",
                "404 Marine Drive, Mumbai, Maharashtra",
                "Maharashtra", // Inter-state
                new BigDecimal("2000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("2100.00"),
                List.of(new InvoiceItemData(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Silk Saree",
                        "SAREE-SLK-BL",
                        "6204",
                        1,
                        new BigDecimal("2000.00"),
                        new BigDecimal("2000.00"),
                        new BigDecimal("5.00")
                ))
        );

        when(invoiceRepository.findByOrderIdAndInvoiceType(orderId, InvoiceType.DISPATCH_TAX_INVOICE))
                .thenReturn(Optional.empty());

        TaxClassificationEntity taxEntity = new TaxClassificationEntity(
                "6204", "Women's apparel", new BigDecimal("5.00"), true);
        when(taxService.getActiveClassificationByHsn("6204")).thenReturn(taxEntity);

        InvoiceSequenceEntity seq = new InvoiceSequenceEntity("2026-27");
        when(invoiceSequenceRepository.findByFiscalYearWithLock(any())).thenReturn(Optional.of(seq));
        when(invoiceRepository.save(any(InvoiceEntity.class))).thenAnswer(inv -> {
            InvoiceEntity e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        InvoiceResponse response = service.generateDispatchInvoice(request);

        // Inter-state: CGST: 0, SGST: 0, IGST: 100
        assertThat(response.cgstAmount()).isEqualByComparingTo("0.00");
        assertThat(response.sgstAmount()).isEqualByComparingTo("0.00");
        assertThat(response.igstAmount()).isEqualByComparingTo("100.00");
        assertThat(response.totalTaxAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("generateDispatchInvoice blocks with TaxConfigurationException when line item has unknown HSN")
    void generateDispatchInvoice_UnknownHsn_ThrowsTaxConfigurationException() {
        UUID orderId = UUID.randomUUID();

        GenerateDispatchInvoiceRequest request = new GenerateDispatchInvoiceRequest(
                orderId,
                "ORD-20261116-1004",
                UUID.randomUUID(),
                "Customer",
                "9999999999",
                "Mumbai",
                "Maharashtra",
                new BigDecimal("1000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("1050.00"),
                List.of(new InvoiceItemData(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Unknown Item",
                        "UNKNOWN-1",
                        "9999",
                        1,
                        new BigDecimal("1000.00"),
                        new BigDecimal("1000.00"),
                        null
                ))
        );

        when(invoiceRepository.findByOrderIdAndInvoiceType(orderId, InvoiceType.DISPATCH_TAX_INVOICE))
                .thenReturn(Optional.empty());
        when(taxService.getActiveClassificationByHsn("9999"))
                .thenThrow(new TaxConfigurationException("Unknown or inactive tax configuration for HSN code: 9999"));

        assertThatThrownBy(() -> service.generateDispatchInvoice(request))
                .isInstanceOf(TaxConfigurationException.class)
                .hasMessageContaining("Unknown or inactive tax configuration for HSN code: 9999");
    }

    @Test
    @DisplayName("calculateFiscalYear calculates Indian financial year correctly across boundaries")
    void calculateFiscalYear_CorrectBorders() {
        assertThat(service.calculateFiscalYear(LocalDate.of(2026, 4, 1))).isEqualTo("2026-27");
        assertThat(service.calculateFiscalYear(LocalDate.of(2026, 11, 16))).isEqualTo("2026-27");
        assertThat(service.calculateFiscalYear(LocalDate.of(2027, 3, 31))).isEqualTo("2026-27");
        assertThat(service.calculateFiscalYear(LocalDate.of(2026, 1, 15))).isEqualTo("2025-26");
    }
}
