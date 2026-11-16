package com.dzfashion.fulfillment;

import com.dzfashion.fulfillment.repository.AgencyCodSettlementRepository;
import com.dzfashion.fulfillment.repository.DailyDispatchBatchRepository;
import com.dzfashion.fulfillment.repository.DeliveryAttemptLogRepository;
import com.dzfashion.fulfillment.repository.OutboxEventRepository;
import com.dzfashion.fulfillment.repository.ShipmentItemRepository;
import com.dzfashion.fulfillment.repository.ShipmentRepository;
import com.dzfashion.fulfillment.repository.TransportAgencyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration,org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"
})
class FulfillmentServiceApplicationTests {

    @MockitoBean
    private TransportAgencyRepository transportAgencyRepository;

    @MockitoBean
    private ShipmentRepository shipmentRepository;

    @MockitoBean
    private ShipmentItemRepository shipmentItemRepository;

    @MockitoBean
    private DeliveryAttemptLogRepository deliveryAttemptLogRepository;

    @MockitoBean
    private DailyDispatchBatchRepository dailyDispatchBatchRepository;

    @MockitoBean
    private AgencyCodSettlementRepository agencyCodSettlementRepository;

    @MockitoBean
    private OutboxEventRepository outboxEventRepository;

    @MockitoBean
    private com.dzfashion.fulfillment.repository.ReturnRequestRepository returnRequestRepository;

    @MockitoBean
    private com.dzfashion.fulfillment.repository.ReturnInspectionRepository returnInspectionRepository;

    @Test
    void contextLoads() {
    }
}
