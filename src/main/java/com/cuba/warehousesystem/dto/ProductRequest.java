package com.cuba.warehousesystem.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank String sku,
        String barcode,
        @NotBlank String name,
        String category,
        @Min(0) Integer minStockLevel,
        @DecimalMin("0.0") BigDecimal weightPerUnitKg,
        @DecimalMin("0.0") BigDecimal volumePerUnitCm3,
        @DecimalMin("0.0") BigDecimal lengthCm,
        @DecimalMin("0.0") BigDecimal widthCm,
        @DecimalMin("0.0") BigDecimal heightCm,
        Boolean isActive
) {
}
