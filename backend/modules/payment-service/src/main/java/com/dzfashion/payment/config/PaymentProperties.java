package com.dzfashion.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
@ConfigurationProperties(prefix = "payment")
public class PaymentProperties {

    private String sellerLegalName = "D & Z Fashion";
    private String sellerGstin = "24AAACD1234E1Z5";
    private String sellerAddress = "Surat Central Warehouse, Ring Road, Surat, Gujarat - 395002";
    private String sellerState = "Gujarat";
    private BigDecimal defaultGstRatePercent = new BigDecimal("5.00");
    private BigDecimal shippingGstRatePercent = new BigDecimal("18.00");

    public String getSellerLegalName() {
        return sellerLegalName;
    }

    public void setSellerLegalName(String sellerLegalName) {
        this.sellerLegalName = sellerLegalName;
    }

    public String getSellerGstin() {
        return sellerGstin;
    }

    public void setSellerGstin(String sellerGstin) {
        this.sellerGstin = sellerGstin;
    }

    public String getSellerAddress() {
        return sellerAddress;
    }

    public void setSellerAddress(String sellerAddress) {
        this.sellerAddress = sellerAddress;
    }

    public String getSellerState() {
        return sellerState;
    }

    public void setSellerState(String sellerState) {
        this.sellerState = sellerState;
    }

    public BigDecimal getDefaultGstRatePercent() {
        return defaultGstRatePercent;
    }

    public void setDefaultGstRatePercent(BigDecimal defaultGstRatePercent) {
        this.defaultGstRatePercent = defaultGstRatePercent;
    }

    public BigDecimal getShippingGstRatePercent() {
        return shippingGstRatePercent;
    }

    public void setShippingGstRatePercent(BigDecimal shippingGstRatePercent) {
        this.shippingGstRatePercent = shippingGstRatePercent;
    }
}
