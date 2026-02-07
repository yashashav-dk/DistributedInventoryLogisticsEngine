package com.inventory.engine.controller;

import com.inventory.engine.dto.ConcurrencyTestResult;
import com.inventory.engine.dto.StockUpdateRequest;
import com.inventory.engine.dto.StockUpdateResponse;
import com.inventory.engine.entity.InventoryItem;
import com.inventory.engine.entity.InventoryLog;
import com.inventory.engine.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public List<InventoryItem> getAllItems() {
        return inventoryService.getAllItems();
    }

    @GetMapping("/warehouse/{warehouseId}")
    public List<InventoryItem> getByWarehouse(@PathVariable String warehouseId) {
        return inventoryService.getItemsByWarehouse(warehouseId);
    }

    @PutMapping("/update-stock")
    public ResponseEntity<StockUpdateResponse> updateStock(
            @Valid @RequestBody StockUpdateRequest request) {
        StockUpdateResponse response = inventoryService.updateStock(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Fires N concurrent deduction requests against a single SKU to demonstrate
     * optimistic locking. Conflicts are expected and counted in the response.
     */
    @PostMapping("/simulate-load")
    public ResponseEntity<ConcurrencyTestResult> simulateLoad(
            @RequestParam(defaultValue = "SKU-001") String sku,
            @RequestParam(defaultValue = "20") int requests) {
        ConcurrencyTestResult result =
                inventoryService.simulateConcurrentUpdates(sku, requests);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/logs")
    public List<InventoryLog> getRecentLogs() {
        return inventoryService.getRecentLogs();
    }

    /**
     * Demonstrates the composite-index-backed query: fetch logs for a given
     * warehouse within a time window.
     */
    @GetMapping("/logs/warehouse/{warehouseId}")
    public List<InventoryLog> getLogsByWarehouse(
            @PathVariable String warehouseId,
            @RequestParam(required = false) Instant start,
            @RequestParam(required = false) Instant end) {
        if (start == null) start = Instant.now().minus(30, ChronoUnit.DAYS);
        if (end == null) end = Instant.now();
        return inventoryService.queryLogsByWarehouseAndTimeRange(warehouseId, start, end);
    }

    @PostMapping("/seed")
    public ResponseEntity<String> seedData() {
        inventoryService.seedData();
        return ResponseEntity.ok("Seed data loaded");
    }
}
