package com.inventory.engine.exception;

public class StockConflictException extends RuntimeException {

    private final String sku;

    public StockConflictException(String sku) {
        super("Concurrent modification detected for SKU: " + sku
                + ". Another transaction updated this item. Retry the operation.");
        this.sku = sku;
    }

    public String getSku() { return sku; }
}
