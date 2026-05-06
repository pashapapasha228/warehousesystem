package com.cuba.warehousesystem.dto;

import com.cuba.warehousesystem.model.OperationStatus;
import com.cuba.warehousesystem.model.OperationType;

import java.time.LocalDateTime;
import java.util.List;

public record OperationResponse(
        Long id,
        String operationNumber,
        OperationType type,
        OperationStatus status,
        Long warehouseId,
        Long userId,
        Long counterpartyId, // Может быть null
        LocalDateTime createdAt,
        LocalDateTime completedAt,
        List<OperationItemResponse> items
) {}

