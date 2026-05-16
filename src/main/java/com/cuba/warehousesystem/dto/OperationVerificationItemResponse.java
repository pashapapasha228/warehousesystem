package com.cuba.warehousesystem.dto;

public record OperationVerificationItemResponse(
        Long id,
        Long operationItemId,
        Long productId,
        String productSku,
        String productName,
        Integer plannedQuantity,
        Integer actualQuantity,
        Integer discrepancyQuantity,
        String reason
) {
}
