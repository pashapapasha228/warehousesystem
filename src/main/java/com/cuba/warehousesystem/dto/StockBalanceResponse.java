package com.cuba.warehousesystem.dto;

import java.time.LocalDateTime;

public record StockBalanceResponse(
        Long productId,
        String productSku,
        String productName,
        Long cellId,
        String cellCode,
        Long warehouseId,
        String warehouseCode,
        Integer quantity,
        Integer reservedQuantity,
        Integer availableQuantity,
        LocalDateTime updatedAt
) {
}
