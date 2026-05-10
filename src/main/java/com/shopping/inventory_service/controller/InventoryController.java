package com.shopping.inventory_service.controller;

import com.shopping.inventory_service.dto.*;
import com.shopping.inventory_service.entity.Inventory;
import com.shopping.inventory_service.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    // POST /api/inventory — create stock record for a product
    @PostMapping
    public ResponseEntity<Inventory> createStock(
            @Valid @RequestBody InventoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inventoryService.createStock(request));
    }

    // GET /api/inventory — get all stock records
    @GetMapping
    public ResponseEntity<List<Inventory>> getAll() {
        return ResponseEntity.ok(inventoryService.getAll());
    }

    // GET /api/inventory/product/{productId} — get stock for one product
    @GetMapping("/product/{productId}")
    public ResponseEntity<StockResponse> getStock(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.getStock(productId));
    }

    // GET /api/inventory/low-stock — get all low stock products
    @GetMapping("/low-stock")
    public ResponseEntity<List<Inventory>> getLowStock() {
        return ResponseEntity.ok(inventoryService.getLowStockProducts());
    }

    // PUT /api/inventory/product/{productId} — admin restocking
    @PutMapping("/product/{productId}")
    public ResponseEntity<Inventory> updateStock(
            @PathVariable Long productId,
            @Valid @RequestBody InventoryRequest request) {
        return ResponseEntity.ok(inventoryService.updateStock(productId, request));
    }

    // POST /api/inventory/deduct — called by Order Service when order is placed
    @PostMapping("/deduct")
    public ResponseEntity<StockResponse> deductStock(
            @Valid @RequestBody StockDeductRequest request) {
        return ResponseEntity.ok(inventoryService.deductStock(request));
    }

    // POST /api/inventory/restore — called by Order Service when order is cancelled
    @PostMapping("/restore")
    public ResponseEntity<StockResponse> restoreStock(
            @Valid @RequestBody StockDeductRequest request) {
        return ResponseEntity.ok(inventoryService.restoreStock(request));
    }
}
