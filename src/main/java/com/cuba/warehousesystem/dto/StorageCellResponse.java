package com.cuba.warehousesystem.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StorageCellResponse(
        Long id,
        Long warehouseId,
        String warehouseCode,
        String code,
        String zone,
        String rack,
        String shelf,
        String level,
        Integer capacityUnits,
        BigDecimal maxWeightKg,
        BigDecimal maxVolumeCm3,
        BigDecimal lengthCm,
        BigDecimal widthCm,
        BigDecimal heightCm,
        BigDecimal currentWeightKg,
        BigDecimal currentVolumeCm3,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
