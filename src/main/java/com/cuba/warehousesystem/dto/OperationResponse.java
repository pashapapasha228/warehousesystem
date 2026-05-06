package com.cuba.warehousesystem.dto;

import com.cuba.warehousesystem.model.OperationSource;
import com.cuba.warehousesystem.model.OperationStatus;
import com.cuba.warehousesystem.model.OperationType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record OperationResponse(
        Long id,
        String operationNumber,
        OperationType type,
        OperationStatus status,
        Long warehouseId,
        String warehouseCode,
        Long createdByUserId,
        Long completedByUserId,
        Long counterpartyId,
        OperationSource source,
        String externalDocumentNumber,
        LocalDate documentDate,
        String comment,
        LocalDateTime createdAt,
        LocalDateTime completedAt,
        List<OperationItemResponse> items
) {
}
