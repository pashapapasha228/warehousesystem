package com.cuba.warehousesystem.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ProductWarehouseMinStockRequest(
        @NotNull Long warehouseId,
        @NotNull @Min(0) Integer minStockLevel
) {
}
