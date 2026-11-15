package com.dzfashion.commerce.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
@ConfigurationProperties(prefix = "commerce")
public class CommerceProperties {

    private BigDecimal gstRatePercent = new BigDecimal("5.00");
    private BigDecimal freeDeliveryThreshold = new BigDecimal("2000.00");
    private BigDecimal perUnitDeliveryFee = new BigDecimal("110.00");
    private String catalogServiceUrl = "http://localhost:8082";
    private String inventoryServiceUrl = "http://localhost:8083";

    public BigDecimal getGstRatePercent() {
        return gstRatePercent;
    }

    public void setGstRatePercent(BigDecimal gstRatePercent) {
        this.gstRatePercent = gstRatePercent;
    }

    public BigDecimal getFreeDeliveryThreshold() {
        return freeDeliveryThreshold;
    }

    public void setFreeDeliveryThreshold(BigDecimal freeDeliveryThreshold) {
        this.freeDeliveryThreshold = freeDeliveryThreshold;
    }

    public BigDecimal getPerUnitDeliveryFee() {
        return perUnitDeliveryFee;
    }

    public void setPerUnitDeliveryFee(BigDecimal perUnitDeliveryFee) {
        this.perUnitDeliveryFee = perUnitDeliveryFee;
    }

    public String getCatalogServiceUrl() {
        return catalogServiceUrl;
    }

    public void setCatalogServiceUrl(String catalogServiceUrl) {
        this.catalogServiceUrl = catalogServiceUrl;
    }

    public String getInventoryServiceUrl() {
        return inventoryServiceUrl;
    }

    public void setInventoryServiceUrl(String inventoryServiceUrl) {
        this.inventoryServiceUrl = inventoryServiceUrl;
    }
}
