package com.inventory.engine.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
    name = "inventory_logs",
    indexes = {
        @Index(name = "idx_warehouse_timestamp", columnList = "warehouseId, timestamp")
    }
)
public class InventoryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String warehouseId;

    @Column(nullable = false, length = 50)
    private String sku;

    @Column(nullable = false, length = 20)
    private String action;

    @Column(nullable = false)
    private Integer quantityChange;

    private Integer resultingQuantity;

    @Column(nullable = false)
    private Instant timestamp;

    private String details;

    public InventoryLog() {}

    public InventoryLog(String warehouseId, String sku, String action,
                        Integer quantityChange, Integer resultingQuantity, String details) {
        this.warehouseId = warehouseId;
        this.sku = sku;
        this.action = action;
        this.quantityChange = quantityChange;
        this.resultingQuantity = resultingQuantity;
        this.details = details;
        this.timestamp = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getWarehouseId() { return warehouseId; }
    public void setWarehouseId(String warehouseId) { this.warehouseId = warehouseId; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public Integer getQuantityChange() { return quantityChange; }
    public void setQuantityChange(Integer quantityChange) { this.quantityChange = quantityChange; }

    public Integer getResultingQuantity() { return resultingQuantity; }
    public void setResultingQuantity(Integer resultingQuantity) { this.resultingQuantity = resultingQuantity; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
