package com.cuba.warehousesystem.dto;

public record OperationItemResponse(
        Long id,
        Long productId,
        Integer quantity,
        Long fromCellId,
        Long toCellId
) {}
