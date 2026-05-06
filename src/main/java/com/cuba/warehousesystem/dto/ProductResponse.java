package com.cuba.warehousesystem.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String sku,
        String barcode,
        String name,
        String category,
        String unitOfMeasure,
        Integer minStockLevel,
        BigDecimal weightPerUnitKg,
        BigDecimal volumePerUnitCm3,
        BigDecimal lengthCm,
        BigDecimal widthCm,
        BigDecimal heightCm,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
