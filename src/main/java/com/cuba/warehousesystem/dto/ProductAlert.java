package com.cuba.warehousesystem.dto;

public record ProductAlert(
        String productName,
        String sku,
        Long warehouseId,
        String warehouseCode,
        Integer currentStock,
        Integer minLevel
) {
}
