package com.shopping.inventory_service.service;

import com.shopping.inventory_service.dto.*;
import com.shopping.inventory_service.entity.Inventory;
import com.shopping.inventory_service.repository.InventoryRepository;
import com.shopping.inventory_service.service.LowStockNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository          inventoryRepository;
    private final LowStockNotificationService  notificationService;

    // Create a stock record for a new product
    public Inventory createStock(InventoryRequest request) {
        if (inventoryRepository.existsByProductId(request.getProductId())) {
            throw new RuntimeException(
                    "Inventory record already exists for product: " + request.getProductId());
        }
        Inventory inventory = new Inventory();
        inventory.setProductId(request.getProductId());
        inventory.setQuantity(request.getQuantity());
        inventory.setLowStockAlert(request.getLowStockAlert());
        return inventoryRepository.save(inventory);
    }

    // Get stock for a specific product
    public StockResponse getStock(Long productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException(
                        "No inventory record found for product: " + productId));

        boolean lowStock = inventory.getQuantity() <= inventory.getLowStockAlert();
        String message = lowStock
                ? "⚠️ Low stock alert — only " + inventory.getQuantity() + " units remaining"
                : "In stock";

        return new StockResponse(
                inventory.getProductId(),
                inventory.getQuantity(),
                lowStock,
                message
        );
    }

    // Get all low stock products
    public List<Inventory> getLowStockProducts() {
        return inventoryRepository.findAll().stream()
                .filter(i -> i.getQuantity() <= i.getLowStockAlert())
                .toList();
    }

    // Get all inventory records
    public List<Inventory> getAll() {
        return inventoryRepository.findAll();
    }

    // Update stock manually (admin restocking)
    public Inventory updateStock(Long productId, InventoryRequest request) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException(
                        "No inventory record found for product: " + productId));

        inventory.setQuantity(request.getQuantity());
        if (request.getLowStockAlert() != null) {
            inventory.setLowStockAlert(request.getLowStockAlert());
        }
        return inventoryRepository.save(inventory);
    }

    // Deduct stock when an order is placed
    @Transactional
    public StockResponse deductStock(StockDeductRequest request) {
        // Use pessimistic write lock to prevent race conditions —
        // two concurrent orders for the last item will queue here
        // instead of both succeeding
        Inventory inventory = inventoryRepository
                .findByProductIdForUpdate(request.getProductId())
                .orElseThrow(() -> new RuntimeException(
                        "No inventory record found for product: " + request.getProductId()));

        // Check if enough stock is available
        if (inventory.getQuantity() < request.getQuantity()) {
            throw new RuntimeException(
                    "Insufficient stock. Available: " + inventory.getQuantity()
                            + ", Requested: " + request.getQuantity());
        }

        boolean wasAboveThreshold = inventory.getQuantity() > inventory.getLowStockAlert();
        inventory.setQuantity(inventory.getQuantity() - request.getQuantity());
        inventoryRepository.save(inventory);

        boolean isNowLowStock = inventory.getQuantity() <= inventory.getLowStockAlert();

        // Only send alert when stock crosses the threshold for the first time
        // (was above, now at or below) — avoids spamming on every order
        if (wasAboveThreshold && isNowLowStock) {
            notificationService.sendLowStockAlert(inventory,
                    "Product #" + inventory.getProductId());
        }

        String message = isNowLowStock
                ? "⚠️ Stock deducted. Low stock alert — only "
                + inventory.getQuantity() + " units remaining"
                : "Stock deducted successfully. Remaining: " + inventory.getQuantity();

        return new StockResponse(
                inventory.getProductId(),
                inventory.getQuantity(),
                isNowLowStock,
                message
        );
    }

    // Restore stock when an order is cancelled
    @Transactional
    public StockResponse restoreStock(StockDeductRequest request) {
        // Use pessimistic write lock to prevent concurrent restore conflicts
        Inventory inventory = inventoryRepository
                .findByProductIdForUpdate(request.getProductId())
                .orElseThrow(() -> new RuntimeException(
                        "No inventory record found for product: " + request.getProductId()));

        inventory.setQuantity(inventory.getQuantity() + request.getQuantity());
        inventoryRepository.save(inventory);

        return new StockResponse(
                inventory.getProductId(),
                inventory.getQuantity(),
                false,
                "Stock restored successfully. New quantity: " + inventory.getQuantity()
        );
    }
}
