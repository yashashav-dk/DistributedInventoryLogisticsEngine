package com.inventory.engine.service;

import com.inventory.engine.dto.ConcurrencyTestResult;
import com.inventory.engine.dto.StockUpdateRequest;
import com.inventory.engine.dto.StockUpdateResponse;
import com.inventory.engine.entity.InventoryItem;
import com.inventory.engine.entity.InventoryLog;
import com.inventory.engine.exception.StockConflictException;
import com.inventory.engine.repository.InventoryLogRepository;
import com.inventory.engine.repository.InventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryRepository inventoryRepository;
    private final InventoryLogRepository logRepository;

    public InventoryService(InventoryRepository inventoryRepository,
                            InventoryLogRepository logRepository) {
        this.inventoryRepository = inventoryRepository;
        this.logRepository = logRepository;
    }

    public List<InventoryItem> getAllItems() {
        return inventoryRepository.findAll();
    }

    public List<InventoryItem> getItemsByWarehouse(String warehouseId) {
        return inventoryRepository.findByWarehouseId(warehouseId);
    }

    /**
     * Updates stock for a given SKU. If another transaction has modified the same
     * row since it was read, JPA's @Version check triggers an
     * ObjectOptimisticLockingFailureException, which we catch and translate
     * into a domain-specific StockConflictException.
     */
    @Transactional
    public StockUpdateResponse updateStock(StockUpdateRequest request) {
        InventoryItem item = inventoryRepository.findBySku(request.getSku())
                .orElseThrow(() -> new IllegalArgumentException(
                        "SKU not found: " + request.getSku()));

        int newQuantity = item.getQuantity() + request.getQuantityChange();
        if (newQuantity < 0) {
            throw new IllegalArgumentException(
                    "Insufficient stock for SKU " + request.getSku()
                    + ". Current: " + item.getQuantity()
                    + ", requested change: " + request.getQuantityChange());
        }

        item.setQuantity(newQuantity);

        try {
            InventoryItem saved = inventoryRepository.saveAndFlush(item);

            logRepository.save(new InventoryLog(
                    saved.getWarehouseId(),
                    saved.getSku(),
                    request.getQuantityChange() >= 0 ? "RESTOCK" : "DEDUCT",
                    request.getQuantityChange(),
                    saved.getQuantity(),
                    "Stock updated via API"
            ));

            return new StockUpdateResponse(
                    true,
                    "Stock updated successfully",
                    saved.getSku(),
                    saved.getQuantity(),
                    saved.getVersion()
            );
        } catch (ObjectOptimisticLockingFailureException ex) {
            log.warn("Optimistic lock conflict on SKU: {}", request.getSku());
            throw new StockConflictException(request.getSku());
        }
    }

    /**
     * Simulates concurrent load by firing multiple stock updates in parallel
     * against the same SKU. Returns a summary showing how many succeeded vs.
     * how many hit optimistic lock conflicts.
     */
    public ConcurrencyTestResult simulateConcurrentUpdates(String sku, int concurrentRequests) {
        long startTime = System.currentTimeMillis();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(
                Math.min(concurrentRequests, 20));

        List<CompletableFuture<Void>> futures = new java.util.ArrayList<>();

        for (int i = 0; i < concurrentRequests; i++) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    updateStock(new StockUpdateRequest(sku, -1));
                    successCount.incrementAndGet();
                } catch (StockConflictException e) {
                    conflictCount.incrementAndGet();
                } catch (Exception e) {
                    conflictCount.incrementAndGet();
                }
            }, executor);
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();

        long durationMs = System.currentTimeMillis() - startTime;

        InventoryItem finalState = inventoryRepository.findBySku(sku)
                .orElse(null);

        ConcurrencyTestResult result = new ConcurrencyTestResult();
        result.setTotalRequests(concurrentRequests);
        result.setSuccessCount(successCount.get());
        result.setConflictCount(conflictCount.get());
        result.setDurationMs(durationMs);

        if (finalState != null) {
            result.setFinalQuantity(finalState.getQuantity());
            result.setFinalVersion(finalState.getVersion());
        }

        log.info("Concurrency test complete: {} total, {} success, {} conflicts, {}ms",
                concurrentRequests, successCount.get(), conflictCount.get(), durationMs);

        return result;
    }

    /**
     * Queries inventory logs using the composite index on (warehouse_id, timestamp).
     */
    public List<InventoryLog> queryLogsByWarehouseAndTimeRange(
            String warehouseId, Instant start, Instant end) {
        return logRepository.findByWarehouseIdAndTimestampBetween(warehouseId, start, end);
    }

    public List<InventoryLog> getRecentLogs() {
        return logRepository.findTop50ByOrderByTimestampDesc();
    }

    @Transactional
    public void seedData() {
        if (inventoryRepository.count() > 0) {
            return;
        }

        String[][] items = {
            {"SKU-001", "Industrial Servo Motor",     "500", "WH-EAST"},
            {"SKU-002", "Hydraulic Pressure Valve",   "320", "WH-EAST"},
            {"SKU-003", "Carbon Fiber Panel 4x8",     "150", "WH-WEST"},
            {"SKU-004", "Titanium Fastener Kit",      "1200","WH-WEST"},
            {"SKU-005", "Precision Ball Bearing 6205", "800", "WH-NORTH"},
            {"SKU-006", "Copper Busbar 200A",         "250", "WH-NORTH"},
            {"SKU-007", "Thermal Interface Compound",  "600", "WH-SOUTH"},
            {"SKU-008", "Stainless Flex Coupling",     "420", "WH-SOUTH"},
        };

        for (String[] row : items) {
            inventoryRepository.save(
                    new InventoryItem(row[0], row[1], Integer.parseInt(row[2]), row[3]));
        }

        log.info("Seeded {} inventory items", items.length);
    }
}
