package com.dzfashion.payment.service;

import com.dzfashion.payment.dto.response.FinancialSummaryReportResponse;
import com.dzfashion.payment.enums.InvoiceType;
import com.dzfashion.payment.enums.PaymentMethod;
import com.dzfashion.payment.enums.PaymentStatus;
import com.dzfashion.payment.repository.InvoiceRepository;
import com.dzfashion.payment.repository.PaymentRepository;
import com.dzfashion.payment.repository.StoreCreditLedgerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@Transactional(readOnly = true)
public class FinancialReportingService {

    private final PaymentRepository paymentRepository;
    private final StoreCreditLedgerRepository storeCreditLedgerRepository;
    private final InvoiceRepository invoiceRepository;

    public FinancialReportingService(PaymentRepository paymentRepository,
                                     StoreCreditLedgerRepository storeCreditLedgerRepository,
                                     InvoiceRepository invoiceRepository) {
        this.paymentRepository = paymentRepository;
        this.storeCreditLedgerRepository = storeCreditLedgerRepository;
        this.invoiceRepository = invoiceRepository;
    }

    public FinancialSummaryReportResponse getFinancialSummaryReport() {
        BigDecimal onlineRevenue = paymentRepository.sumAmountByMethodAndStatus(
                PaymentMethod.SIMULATED_ONLINE, PaymentStatus.SUCCESS);
        BigDecimal codCollected = paymentRepository.sumAmountByMethodAndStatus(
                PaymentMethod.COD, PaymentStatus.SUCCESS);
        BigDecimal codPending = paymentRepository.sumAmountByMethodAndStatus(
                PaymentMethod.COD, PaymentStatus.PENDING);
        BigDecimal storeCreditOutstanding = storeCreditLedgerRepository.calculateTotalOutstandingCredit();
        BigDecimal invoicedTax = invoiceRepository.sumTaxAmountByInvoiceType(
                InvoiceType.DISPATCH_TAX_INVOICE);
        long completedOrders = paymentRepository.countByStatus(PaymentStatus.SUCCESS);

        return new FinancialSummaryReportResponse(
                onlineRevenue != null ? onlineRevenue.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                codCollected != null ? codCollected.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                codPending != null ? codPending.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                storeCreditOutstanding != null ? storeCreditOutstanding.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                invoicedTax != null ? invoicedTax.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                completedOrders
        );
    }
}
