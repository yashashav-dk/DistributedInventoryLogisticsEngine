package com.inventory.engine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class StockUpdateRequest {

    @NotBlank
    private String sku;

    @NotNull
    private Integer quantityChange;

    public StockUpdateRequest() {}

    public StockUpdateRequest(String sku, Integer quantityChange) {
        this.sku = sku;
        this.quantityChange = quantityChange;
    }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public Integer getQuantityChange() { return quantityChange; }
    public void setQuantityChange(Integer quantityChange) { this.quantityChange = quantityChange; }
}
