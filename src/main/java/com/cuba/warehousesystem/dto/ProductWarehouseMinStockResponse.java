package com.cuba.warehousesystem.dto;

public record ProductWarehouseMinStockResponse(
        Long productId,
        Long warehouseId,
        String warehouseCode,
        String warehouseName,
        Integer minStockLevel
) {
}
