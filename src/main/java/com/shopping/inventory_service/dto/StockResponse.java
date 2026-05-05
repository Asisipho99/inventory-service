package com.shopping.inventory_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StockResponse {
    private Long productId;
    private Integer quantity;
    private boolean lowStock;
    private String message;
}
