package com.cuba.warehousesystem.dto;

import java.math.BigDecimal;

public record OperationItemResponse(
        Long id,
        Long productId,
        String productSku,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        String unitOfMeasure,
        Long fromCellId,
        String fromCellCode,
        Long toCellId,
        String toCellCode
) {
}
