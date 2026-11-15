package com.dzfashion.inventory;

import com.dzfashion.inventory.repository.LowStockAlertRepository;
import com.dzfashion.inventory.repository.StockMovementRepository;
import com.dzfashion.inventory.repository.StockReservationRepository;
import com.dzfashion.inventory.repository.WarehouseRepository;
import com.dzfashion.inventory.repository.WarehouseStockRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration,org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"
})
class InventoryServiceApplicationTests {

    @MockitoBean
    private WarehouseStockRepository warehouseStockRepository;

    @MockitoBean
    private WarehouseRepository warehouseRepository;

    @MockitoBean
    private StockReservationRepository stockReservationRepository;

    @MockitoBean
    private StockMovementRepository stockMovementRepository;

    @MockitoBean
    private LowStockAlertRepository lowStockAlertRepository;

    @Test
    void contextLoads() {
    }
}
