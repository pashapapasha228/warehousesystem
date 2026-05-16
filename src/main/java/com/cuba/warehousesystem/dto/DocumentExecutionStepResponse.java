package com.cuba.warehousesystem.dto;

import com.cuba.warehousesystem.model.DocumentExecutionStage;
import com.cuba.warehousesystem.model.DocumentExecutionStatus;

import java.time.LocalDateTime;

public record DocumentExecutionStepResponse(
        Long id,
        Long operationId,
        Long ediMessageId,
        DocumentExecutionStage stage,
        DocumentExecutionStatus status,
        String details,
        String createdBy,
        LocalDateTime createdAt
) {
}
