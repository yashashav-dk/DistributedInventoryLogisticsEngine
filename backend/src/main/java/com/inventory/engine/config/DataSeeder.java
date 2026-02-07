package com.inventory.engine.config;

import com.inventory.engine.service.InventoryService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final InventoryService inventoryService;

    public DataSeeder(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Override
    public void run(String... args) {
        inventoryService.seedData();
    }
}
