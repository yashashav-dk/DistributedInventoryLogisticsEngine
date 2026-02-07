package com.inventory.engine.repository;

import com.inventory.engine.entity.InventoryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface InventoryLogRepository extends JpaRepository<InventoryLog, Long> {

    /**
     * Leverages the composite index (warehouse_id, timestamp) for
     * high-performance range queries across warehouse activity logs.
     */
    List<InventoryLog> findByWarehouseIdAndTimestampBetween(
            String warehouseId, Instant start, Instant end);

    List<InventoryLog> findBySkuOrderByTimestampDesc(String sku);

    List<InventoryLog> findTop50ByOrderByTimestampDesc();
}
