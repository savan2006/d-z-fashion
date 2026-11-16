package com.dzfashion.payment;

import com.dzfashion.payment.repository.CodSettlementBatchRepository;
import com.dzfashion.payment.repository.InvoiceRepository;
import com.dzfashion.payment.repository.InvoiceSequenceRepository;
import com.dzfashion.payment.repository.OutboxEventRepository;
import com.dzfashion.payment.repository.PaymentRepository;
import com.dzfashion.payment.repository.PromotionRepository;
import com.dzfashion.payment.repository.StoreCreditLedgerRepository;
import com.dzfashion.payment.repository.TaxClassificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration,org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"
})
class PaymentServiceApplicationTests {

    @MockitoBean
    private PaymentRepository paymentRepository;

    @MockitoBean
    private StoreCreditLedgerRepository storeCreditLedgerRepository;

    @MockitoBean
    private PromotionRepository promotionRepository;

    @MockitoBean
    private TaxClassificationRepository taxClassificationRepository;

    @MockitoBean
    private InvoiceRepository invoiceRepository;

    @MockitoBean
    private InvoiceSequenceRepository invoiceSequenceRepository;

    @MockitoBean
    private CodSettlementBatchRepository codSettlementBatchRepository;

    @MockitoBean
    private OutboxEventRepository outboxEventRepository;

    @Test
    void contextLoads() {
    }
}
