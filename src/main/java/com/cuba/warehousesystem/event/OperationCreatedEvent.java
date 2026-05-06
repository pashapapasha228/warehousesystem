package com.cuba.warehousesystem.event;

import com.cuba.warehousesystem.model.OperationType;

import java.time.LocalDateTime;

public record OperationCreatedEvent(
        Long operationId,
        String operationNumber,
        OperationType type,
        Long warehouseId,
        String username,
        LocalDateTime occurredAt
) {
}
