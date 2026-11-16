package com.dzfashion.fulfillment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "fulfillment")
public class FulfillmentProperties {

    private String warehouseName = "Surat Central Warehouse";
    private String warehouseAddress = "Surat Central Warehouse, Ring Road, Surat, Gujarat - 395002";
    private String warehouseCity = "Surat";
    private String warehouseState = "Gujarat";
    private String warehousePincode = "395002";
    private int maxDeliveryAttempts = 2;
    private int returnWindowDays = 3;

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public String getWarehouseAddress() {
        return warehouseAddress;
    }

    public void setWarehouseAddress(String warehouseAddress) {
        this.warehouseAddress = warehouseAddress;
    }

    public String getWarehouseCity() {
        return warehouseCity;
    }

    public void setWarehouseCity(String warehouseCity) {
        this.warehouseCity = warehouseCity;
    }

    public String getWarehouseState() {
        return warehouseState;
    }

    public void setWarehouseState(String warehouseState) {
        this.warehouseState = warehouseState;
    }

    public String getWarehousePincode() {
        return warehousePincode;
    }

    public void setWarehousePincode(String warehousePincode) {
        this.warehousePincode = warehousePincode;
    }

    public int getMaxDeliveryAttempts() {
        return maxDeliveryAttempts;
    }

    public void setMaxDeliveryAttempts(int maxDeliveryAttempts) {
        this.maxDeliveryAttempts = maxDeliveryAttempts;
    }

    public int getReturnWindowDays() {
        return returnWindowDays;
    }

    public void setReturnWindowDays(int returnWindowDays) {
        this.returnWindowDays = returnWindowDays;
    }
}
