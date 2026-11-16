package com.dzfashion.payment.service;

import com.dzfashion.payment.dto.response.FinancialSummaryReportResponse;
import com.dzfashion.payment.enums.InvoiceType;
import com.dzfashion.payment.enums.PaymentMethod;
import com.dzfashion.payment.enums.PaymentStatus;
import com.dzfashion.payment.repository.InvoiceRepository;
import com.dzfashion.payment.repository.PaymentRepository;
import com.dzfashion.payment.repository.StoreCreditLedgerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialReportingServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private StoreCreditLedgerRepository storeCreditLedgerRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    private FinancialReportingService service;

    @BeforeEach
    void setUp() {
        service = new FinancialReportingService(paymentRepository, storeCreditLedgerRepository, invoiceRepository);
    }

    @Test
    @DisplayName("getFinancialSummaryReport aggregates online revenue, COD collected, COD pending, store credit liability, and invoiced tax")
    void getFinancialSummaryReport_Success() {
        when(paymentRepository.sumAmountByMethodAndStatus(PaymentMethod.SIMULATED_ONLINE, PaymentStatus.SUCCESS))
                .thenReturn(new BigDecimal("125000.00"));
        when(paymentRepository.sumAmountByMethodAndStatus(PaymentMethod.COD, PaymentStatus.SUCCESS))
                .thenReturn(new BigDecimal("45000.00"));
        when(paymentRepository.sumAmountByMethodAndStatus(PaymentMethod.COD, PaymentStatus.PENDING))
                .thenReturn(new BigDecimal("12000.00"));
        when(storeCreditLedgerRepository.calculateTotalOutstandingCredit())
                .thenReturn(new BigDecimal("3500.00"));
        when(invoiceRepository.sumTaxAmountByInvoiceType(InvoiceType.DISPATCH_TAX_INVOICE))
                .thenReturn(new BigDecimal("8500.00"));
        when(paymentRepository.countByStatus(PaymentStatus.SUCCESS))
                .thenReturn(150L);

        FinancialSummaryReportResponse report = service.getFinancialSummaryReport();

        assertThat(report.totalSimulatedOnlineRevenue()).isEqualByComparingTo("125000.00");
        assertThat(report.totalCodCollectedRevenue()).isEqualByComparingTo("45000.00");
        assertThat(report.totalCodPendingReceivable()).isEqualByComparingTo("12000.00");
        assertThat(report.totalStoreCreditOutstanding()).isEqualByComparingTo("3500.00");
        assertThat(report.totalInvoicedGstTax()).isEqualByComparingTo("8500.00");
        assertThat(report.totalCompletedOrders()).isEqualTo(150L);
    }
}
