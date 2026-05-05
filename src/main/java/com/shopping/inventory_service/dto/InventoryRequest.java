package com.shopping.inventory_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InventoryRequest {

    @NotNull
    private Long productId;

    @Min(0)
    private Integer quantity;

    @Min(1)
    private Integer lowStockAlert = 10;
}