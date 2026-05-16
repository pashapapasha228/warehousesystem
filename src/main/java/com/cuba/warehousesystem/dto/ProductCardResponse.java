package com.cuba.warehousesystem.dto;

import java.util.List;

public record ProductCardResponse(
        ProductResponse product,
        Integer totalQuantity,
        Integer totalReservedQuantity,
        Integer totalAvailableQuantity,
        List<WarehouseAggregate> warehouseAggregates,
        List<ProductWarehouseMinStockResponse> minStockLevels,
        List<Placement> placements,
        List<OperationResponse> recentOperations
) {
    public record WarehouseAggregate(
            Long warehouseId,
            String warehouseCode,
            Integer quantity,
            Integer reservedQuantity,
            Integer availableQuantity,
            Integer minStockLevel
    ) {
    }

    public record Placement(
            Long warehouseId,
            String warehouseCode,
            Long cellId,
            String cellCode,
            Integer quantity,
            Integer reservedQuantity,
            Integer availableQuantity
    ) {
    }
}
