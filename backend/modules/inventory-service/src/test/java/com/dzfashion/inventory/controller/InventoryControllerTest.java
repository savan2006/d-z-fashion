package com.dzfashion.inventory.controller;

import com.dzfashion.inventory.dto.response.StockResponse;
import com.dzfashion.inventory.security.JwtAuthenticationFilter;
import com.dzfashion.inventory.security.JwtTokenService;
import com.dzfashion.inventory.security.SecurityConfig;
import com.dzfashion.inventory.service.InventoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InventoryController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryService inventoryService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @Test
    @DisplayName("Public GET /api/v1/inventory/variants/{id} returns stock balance")
    void getStockByVariantIdPublic() throws Exception {
        UUID variantId = UUID.randomUUID();
        when(inventoryService.getStockByVariantId(variantId)).thenReturn(
                new StockResponse(variantId, "DZF-TEST-SKU", 20, 2, 18, 0, 5, Instant.now())
        );

        mockMvc.perform(get("/api/v1/inventory/variants/" + variantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("DZF-TEST-SKU"))
                .andExpect(jsonPath("$.availableQty").value(18))
                .andExpect(jsonPath("$.physicalQty").value(20));
    }

    @Test
    @DisplayName("Public GET /api/v1/inventory/sku/{sku} returns stock balance")
    void getStockBySkuPublic() throws Exception {
        UUID variantId = UUID.randomUUID();
        when(inventoryService.getStockBySku("DZF-KUR-MRN-M")).thenReturn(
                new StockResponse(variantId, "DZF-KUR-MRN-M", 15, 0, 15, 0, 5, Instant.now())
        );

        mockMvc.perform(get("/api/v1/inventory/sku/DZF-KUR-MRN-M"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("DZF-KUR-MRN-M"))
                .andExpect(jsonPath("$.availableQty").value(15));
    }
}
