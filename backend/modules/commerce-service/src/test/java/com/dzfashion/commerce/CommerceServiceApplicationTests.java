package com.dzfashion.commerce;

import com.dzfashion.commerce.repository.CartItemRepository;
import com.dzfashion.commerce.repository.CartRepository;
import com.dzfashion.commerce.repository.CodServiceablePincodeRepository;
import com.dzfashion.commerce.repository.OrderClaimHistoryRepository;
import com.dzfashion.commerce.repository.OrderItemRepository;
import com.dzfashion.commerce.repository.OrderRepository;
import com.dzfashion.commerce.repository.OrderShortageIncidentRepository;
import com.dzfashion.commerce.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration,org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"
})
class CommerceServiceApplicationTests {

    @MockitoBean
    private CartRepository cartRepository;

    @MockitoBean
    private CartItemRepository cartItemRepository;

    @MockitoBean
    private OrderRepository orderRepository;

    @MockitoBean
    private OrderItemRepository orderItemRepository;

    @MockitoBean
    private CodServiceablePincodeRepository codServiceablePincodeRepository;

    @MockitoBean
    private OrderClaimHistoryRepository orderClaimHistoryRepository;

    @MockitoBean
    private OrderShortageIncidentRepository orderShortageIncidentRepository;

    @MockitoBean
    private OutboxEventRepository outboxEventRepository;

    @Test
    void contextLoads() {
    }
}
