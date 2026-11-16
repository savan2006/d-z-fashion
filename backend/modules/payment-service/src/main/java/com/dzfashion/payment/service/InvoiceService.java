package com.dzfashion.payment.service;

import com.dzfashion.payment.config.PaymentProperties;
import com.dzfashion.payment.dto.request.GenerateDispatchInvoiceRequest;
import com.dzfashion.payment.dto.request.GenerateReceiptRequest;
import com.dzfashion.payment.dto.request.InvoiceItemData;
import com.dzfashion.payment.dto.response.InvoiceItemResponse;
import com.dzfashion.payment.dto.response.InvoiceResponse;
import com.dzfashion.payment.entity.InvoiceEntity;
import com.dzfashion.payment.entity.InvoiceItemEntity;
import com.dzfashion.payment.entity.InvoiceSequenceEntity;
import com.dzfashion.payment.entity.OutboxEventEntity;
import com.dzfashion.payment.entity.TaxClassificationEntity;
import com.dzfashion.payment.enums.InvoiceType;
import com.dzfashion.payment.exception.InvalidOperationException;
import com.dzfashion.payment.exception.ResourceNotFoundException;
import com.dzfashion.payment.exception.TaxConfigurationException;
import com.dzfashion.payment.repository.InvoiceRepository;
import com.dzfashion.payment.repository.InvoiceSequenceRepository;
import com.dzfashion.payment.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);

    private final InvoiceRepository invoiceRepository;
    private final InvoiceSequenceRepository invoiceSequenceRepository;
    private final TaxService taxService;
    private final OutboxEventRepository outboxEventRepository;
    private final PaymentProperties paymentProperties;
    private final ObjectMapper objectMapper;

    public InvoiceService(InvoiceRepository invoiceRepository,
                          InvoiceSequenceRepository invoiceSequenceRepository,
                          TaxService taxService,
                          OutboxEventRepository outboxEventRepository,
                          PaymentProperties paymentProperties,
                          ObjectMapper objectMapper) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceSequenceRepository = invoiceSequenceRepository;
        this.taxService = taxService;
        this.outboxEventRepository = outboxEventRepository;
        this.paymentProperties = paymentProperties;
        this.objectMapper = objectMapper;
    }

    public InvoiceResponse generateReceipt(GenerateReceiptRequest request) {
        Optional<InvoiceEntity> existing = invoiceRepository.findByOrderIdAndInvoiceType(
                request.orderId(), InvoiceType.RECEIPT);
        if (existing.isPresent()) {
            log.info("Receipt already exists for order {}: {}", request.orderId(), existing.get().getInvoiceNumber());
            return mapToResponse(existing.get());
        }

        String invoiceNumber = getNextInvoiceNumber(InvoiceType.RECEIPT);

        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setOrderId(request.orderId());
        invoice.setOrderNumber(request.orderNumber().trim());
        invoice.setInvoiceType(InvoiceType.RECEIPT);
        invoice.setSellerLegalName(paymentProperties.getSellerLegalName());
        invoice.setSellerGstin(paymentProperties.getSellerGstin());
        invoice.setSellerAddress(paymentProperties.getSellerAddress());
        invoice.setSellerState(paymentProperties.getSellerState());
        invoice.setCustomerId(request.customerId());
        invoice.setCustomerName(request.customerName().trim());
        invoice.setCustomerPhone(request.customerPhone().trim());
        invoice.setShippingAddress(request.shippingAddress().trim());
        invoice.setPlaceOfSupplyState(request.placeOfSupplyState().trim());
        invoice.setTaxableAmount(request.taxableAmount().setScale(2, RoundingMode.HALF_UP));
        invoice.setTotalTaxAmount(request.gstAmount().setScale(2, RoundingMode.HALF_UP));
        invoice.setDeliveryCharge(request.deliveryCharge().setScale(2, RoundingMode.HALF_UP));
        invoice.setDiscountAmount(request.discountAmount().setScale(2, RoundingMode.HALF_UP));
        invoice.setTotalAmount(request.totalAmount().setScale(2, RoundingMode.HALF_UP));
        invoice.setIssuedAt(Instant.now());

        boolean isIntraState = paymentProperties.getSellerState().trim().equalsIgnoreCase(request.placeOfSupplyState().trim());
        if (isIntraState) {
            BigDecimal halfGst = request.gstAmount().divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
            invoice.setCgstAmount(halfGst);
            invoice.setSgstAmount(request.gstAmount().subtract(halfGst));
            invoice.setIgstAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        } else {
            invoice.setCgstAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            invoice.setSgstAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            invoice.setIgstAmount(request.gstAmount().setScale(2, RoundingMode.HALF_UP));
        }

        for (InvoiceItemData itemData : request.items()) {
            InvoiceItemEntity itemEntity = new InvoiceItemEntity(
                    itemData.productId(),
                    itemData.variantId(),
                    itemData.productName().trim(),
                    itemData.sku().trim(),
                    itemData.hsnCode() != null && !itemData.hsnCode().isBlank() ? itemData.hsnCode().trim() : "6204",
                    itemData.quantity(),
                    itemData.unitPrice().setScale(2, RoundingMode.HALF_UP),
                    itemData.taxableAmount().setScale(2, RoundingMode.HALF_UP),
                    itemData.gstRatePercent() != null ? itemData.gstRatePercent().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    itemData.taxableAmount().setScale(2, RoundingMode.HALF_UP)
            );
            invoice.addItem(itemEntity);
        }

        InvoiceEntity saved = invoiceRepository.save(invoice);
        saveOutboxEvent("INVOICE", saved.getId().toString(), "RECEIPT_GENERATED", saved);
        log.info("Generated receipt {} for order {}", saved.getInvoiceNumber(), saved.getOrderId());
        return mapToResponse(saved);
    }

    public InvoiceResponse generateDispatchInvoice(GenerateDispatchInvoiceRequest request) {
        Optional<InvoiceEntity> existing = invoiceRepository.findByOrderIdAndInvoiceType(
                request.orderId(), InvoiceType.DISPATCH_TAX_INVOICE);
        if (existing.isPresent()) {
            log.info("Dispatch tax invoice already exists for order {}: {}", request.orderId(), existing.get().getInvoiceNumber());
            return mapToResponse(existing.get());
        }

        if (request.items() == null || request.items().isEmpty()) {
            throw new InvalidOperationException("Cannot generate dispatch tax invoice without line items");
        }

        for (InvoiceItemData itemData : request.items()) {
            if (itemData.hsnCode() == null || itemData.hsnCode().isBlank()) {
                throw new TaxConfigurationException("Missing HSN code for item: " + itemData.productName() + " (SKU: " + itemData.sku() + ")");
            }
            taxService.getActiveClassificationByHsn(itemData.hsnCode().trim());
        }

        boolean isIntraState = paymentProperties.getSellerState().trim().equalsIgnoreCase(request.placeOfSupplyState().trim());
        String invoiceNumber = getNextInvoiceNumber(InvoiceType.DISPATCH_TAX_INVOICE);

        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setOrderId(request.orderId());
        invoice.setOrderNumber(request.orderNumber().trim());
        invoice.setInvoiceType(InvoiceType.DISPATCH_TAX_INVOICE);
        invoice.setSellerLegalName(paymentProperties.getSellerLegalName());
        invoice.setSellerGstin(paymentProperties.getSellerGstin());
        invoice.setSellerAddress(paymentProperties.getSellerAddress());
        invoice.setSellerState(paymentProperties.getSellerState());
        invoice.setCustomerId(request.customerId());
        invoice.setCustomerName(request.customerName().trim());
        invoice.setCustomerPhone(request.customerPhone().trim());
        invoice.setShippingAddress(request.shippingAddress().trim());
        invoice.setPlaceOfSupplyState(request.placeOfSupplyState().trim());
        invoice.setIssuedAt(Instant.now());

        BigDecimal totalTaxable = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalCgst = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalSgst = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalIgst = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        for (InvoiceItemData itemData : request.items()) {
            if (itemData.hsnCode() == null || itemData.hsnCode().isBlank()) {
                throw new TaxConfigurationException("Missing HSN code for item: " + itemData.productName() + " (SKU: " + itemData.sku() + ")");
            }

            TaxClassificationEntity taxClassification = taxService.getActiveClassificationByHsn(itemData.hsnCode().trim());
            BigDecimal gstRate = taxClassification.getGstRatePercent();

            BigDecimal itemTaxable = itemData.taxableAmount().setScale(2, RoundingMode.HALF_UP);
            totalTaxable = totalTaxable.add(itemTaxable);

            BigDecimal itemTax = itemTaxable.multiply(gstRate)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            BigDecimal itemCgst = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            BigDecimal itemSgst = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            BigDecimal itemIgst = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

            if (isIntraState) {
                itemCgst = itemTax.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
                itemSgst = itemTax.subtract(itemCgst);
                totalCgst = totalCgst.add(itemCgst);
                totalSgst = totalSgst.add(itemSgst);
            } else {
                itemIgst = itemTax;
                totalIgst = totalIgst.add(itemIgst);
            }

            BigDecimal itemTotal = itemTaxable.add(itemTax).setScale(2, RoundingMode.HALF_UP);

            InvoiceItemEntity itemEntity = new InvoiceItemEntity(
                    itemData.productId(),
                    itemData.variantId(),
                    itemData.productName().trim(),
                    itemData.sku().trim(),
                    taxClassification.getHsnCode(),
                    itemData.quantity(),
                    itemData.unitPrice().setScale(2, RoundingMode.HALF_UP),
                    itemTaxable,
                    gstRate.setScale(2, RoundingMode.HALF_UP),
                    itemCgst,
                    itemSgst,
                    itemIgst,
                    itemTotal
            );
            invoice.addItem(itemEntity);
        }

        BigDecimal deliveryCharge = request.deliveryCharge() != null
                ? request.deliveryCharge().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal discountAmount = request.discountAmount() != null
                ? request.discountAmount().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalTaxAmount = totalCgst.add(totalSgst).add(totalIgst).setScale(2, RoundingMode.HALF_UP);

        invoice.setTaxableAmount(request.taxableAmount() != null
                ? request.taxableAmount().setScale(2, RoundingMode.HALF_UP)
                : totalTaxable);
        invoice.setCgstAmount(totalCgst);
        invoice.setSgstAmount(totalSgst);
        invoice.setIgstAmount(totalIgst);
        invoice.setTotalTaxAmount(totalTaxAmount);
        invoice.setDeliveryCharge(deliveryCharge);
        invoice.setDiscountAmount(discountAmount);
        invoice.setTotalAmount(request.totalAmount() != null
                ? request.totalAmount().setScale(2, RoundingMode.HALF_UP)
                : totalTaxable.add(totalTaxAmount).add(deliveryCharge).subtract(discountAmount));

        InvoiceEntity saved = invoiceRepository.save(invoice);
        saveOutboxEvent("INVOICE", saved.getId().toString(), "TAX_INVOICE_GENERATED", saved);
        log.info("Generated dispatch GST tax invoice {} for order {}", saved.getInvoiceNumber(), saved.getOrderId());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceById(UUID id) {
        InvoiceEntity entity = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with ID: " + id));
        return mapToResponse(entity);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceByNumber(String invoiceNumber) {
        InvoiceEntity entity = invoiceRepository.findByInvoiceNumber(invoiceNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with number: " + invoiceNumber));
        return mapToResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getInvoicesForOrder(UUID orderId) {
        return invoiceRepository.findByOrderIdOrderByIssuedAtAsc(orderId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<InvoiceResponse> getCustomerInvoices(UUID customerId, Pageable pageable) {
        return invoiceRepository.findByCustomerIdOrderByIssuedAtDesc(customerId, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<InvoiceResponse> getAllInvoices(Pageable pageable) {
        return invoiceRepository.findAllByOrderByIssuedAtDesc(pageable)
                .map(this::mapToResponse);
    }

    public String calculateFiscalYear(LocalDate date) {
        int year = date.getYear();
        int month = date.getMonthValue();
        int startYear;
        int endYear;
        if (month >= 4) {
            startYear = year;
            endYear = (year + 1) % 100;
        } else {
            startYear = year - 1;
            endYear = year % 100;
        }
        return String.format("%d-%02d", startYear, endYear);
    }

    private String getNextInvoiceNumber(InvoiceType type) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        String fiscalYear = calculateFiscalYear(today);

        InvoiceSequenceEntity seq = invoiceSequenceRepository.findByFiscalYearWithLock(fiscalYear)
                .orElseGet(() -> {
                    try {
                        return invoiceSequenceRepository.saveAndFlush(new InvoiceSequenceEntity(fiscalYear));
                    } catch (Exception ex) {
                        return invoiceSequenceRepository.findByFiscalYearWithLock(fiscalYear)
                                .orElseThrow(() -> new IllegalStateException("Failed to initialize invoice sequence for " + fiscalYear));
                    }
                });

        if (type == InvoiceType.RECEIPT) {
            long next = seq.incrementReceiptSequence();
            invoiceSequenceRepository.save(seq);
            return String.format("RCP/%s/%06d", fiscalYear, next);
        } else {
            long next = seq.incrementTaxInvoiceSequence();
            invoiceSequenceRepository.save(seq);
            return String.format("INV/%s/%06d", fiscalYear, next);
        }
    }

    private InvoiceResponse mapToResponse(InvoiceEntity entity) {
        List<InvoiceItemResponse> itemResponses = new ArrayList<>();
        if (entity.getItems() != null) {
            for (InvoiceItemEntity item : entity.getItems()) {
                itemResponses.add(new InvoiceItemResponse(
                        item.getId(),
                        item.getProductId(),
                        item.getVariantId(),
                        item.getProductName(),
                        item.getSku(),
                        item.getHsnCode(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getTaxableAmount(),
                        item.getGstRatePercent(),
                        item.getCgstAmount(),
                        item.getSgstAmount(),
                        item.getIgstAmount(),
                        item.getTotalAmount()
                ));
            }
        }

        return new InvoiceResponse(
                entity.getId(),
                entity.getInvoiceNumber(),
                entity.getOrderId(),
                entity.getOrderNumber(),
                entity.getInvoiceType(),
                entity.getSellerLegalName(),
                entity.getSellerGstin(),
                entity.getSellerAddress(),
                entity.getSellerState(),
                entity.getCustomerId(),
                entity.getCustomerName(),
                entity.getCustomerPhone(),
                entity.getShippingAddress(),
                entity.getPlaceOfSupplyState(),
                entity.getTaxableAmount(),
                entity.getCgstAmount(),
                entity.getSgstAmount(),
                entity.getIgstAmount(),
                entity.getTotalTaxAmount(),
                entity.getDeliveryCharge(),
                entity.getDiscountAmount(),
                entity.getTotalAmount(),
                entity.getCurrency(),
                entity.getIssuedAt(),
                itemResponses
        );
    }

    private void saveOutboxEvent(String aggregateType, String aggregateId, String eventType, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            outboxEventRepository.save(new OutboxEventEntity(aggregateType, aggregateId, eventType, json));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox event for {} {}", aggregateType, aggregateId, e);
            throw new RuntimeException("Failed to persist outbox event", e);
        }
    }
}
