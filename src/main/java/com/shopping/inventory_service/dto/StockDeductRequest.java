package com.shopping.inventory_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StockDeductRequest {

    @NotNull
    private Long productId;

    @Min(1)
    private Integer quantity;
}
