package com.cuba.warehousesystem.dto;

import com.cuba.warehousesystem.model.ProductCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank String sku,
        String barcode,
        @NotBlank String name,
        ProductCategory category,
        @DecimalMin("0.0") BigDecimal weightPerUnitKg,
        @DecimalMin("0.0") BigDecimal lengthCm,
        @DecimalMin("0.0") BigDecimal widthCm,
        @DecimalMin("0.0") BigDecimal heightCm,
        Boolean isActive
) {
}
