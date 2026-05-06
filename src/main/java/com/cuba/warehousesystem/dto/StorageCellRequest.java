package com.cuba.warehousesystem.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record StorageCellRequest(
        @NotNull Long warehouseId,
        @NotBlank String code,
        String zone,
        String rack,
        String shelf,
        String level,
        @Min(0) Integer capacityUnits,
        @DecimalMin("0.0") BigDecimal maxWeightKg,
        @DecimalMin("0.0") BigDecimal maxVolumeCm3,
        @DecimalMin("0.0") BigDecimal lengthCm,
        @DecimalMin("0.0") BigDecimal widthCm,
        @DecimalMin("0.0") BigDecimal heightCm,
        Boolean isActive
) {
}
