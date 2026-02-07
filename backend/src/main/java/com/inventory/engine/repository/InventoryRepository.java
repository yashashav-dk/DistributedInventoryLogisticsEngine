package com.inventory.engine.repository;

import com.inventory.engine.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<InventoryItem, Long> {

    Optional<InventoryItem> findBySku(String sku);

    List<InventoryItem> findByWarehouseId(String warehouseId);
}
