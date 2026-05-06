package com.cuba.warehousesystem.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MovementReport(List<MovementItem> movements) {
    public record MovementItem(
            String operationNumber,
            String operationType,
            String productSku,
            String productName,
            Integer quantity,
            String fromCellCode,
            String toCellCode,
            LocalDateTime completedAt
    ) {}
}
