package com.inventory.engine.dto;

public class StockUpdateResponse {

    private boolean success;
    private String message;
    private String sku;
    private Integer newQuantity;
    private Long version;

    public StockUpdateResponse() {}

    public StockUpdateResponse(boolean success, String message, String sku,
                                Integer newQuantity, Long version) {
        this.success = success;
        this.message = message;
        this.sku = sku;
        this.newQuantity = newQuantity;
        this.version = version;
    }

    public static StockUpdateResponse conflict(String sku, String message) {
        return new StockUpdateResponse(false, message, sku, null, null);
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public Integer getNewQuantity() { return newQuantity; }
    public void setNewQuantity(Integer newQuantity) { this.newQuantity = newQuantity; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
