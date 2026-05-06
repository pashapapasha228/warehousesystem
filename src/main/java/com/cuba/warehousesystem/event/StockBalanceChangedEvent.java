package com.cuba.warehousesystem.event;

import com.cuba.warehousesystem.model.OperationType;

import java.time.LocalDateTime;

public record StockBalanceChangedEvent(
        Long operationId,
        Long productId,
        Long cellId,
        Integer previousQuantity,
        Integer newQuantity,
        OperationType operationType,
        String username,
        LocalDateTime occurredAt
) {
}
