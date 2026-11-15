package com.dzfashion.catalog;

import com.dzfashion.catalog.repository.CategoryRepository;
import com.dzfashion.catalog.repository.MediaAssetRepository;
import com.dzfashion.catalog.repository.ProductRepository;
import com.dzfashion.catalog.repository.ProductRevisionRepository;
import com.dzfashion.catalog.repository.ProductVariantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration,org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"
})
class CatalogServiceApplicationTests {

    @MockitoBean
    private CategoryRepository categoryRepository;

    @MockitoBean
    private ProductRepository productRepository;

    @MockitoBean
    private ProductVariantRepository productVariantRepository;

    @MockitoBean
    private ProductRevisionRepository productRevisionRepository;

    @MockitoBean
    private MediaAssetRepository mediaAssetRepository;

    @Test
    void contextLoads() {
    }
}
