package com.cuba.warehousesystem.dto;

import com.cuba.warehousesystem.model.EdiAuditStatus;

import java.time.LocalDateTime;

public record EdiAuditLogResponse(
        Long id,
        Long ediMessageId,
        String stage,
        EdiAuditStatus status,
        String details,
        LocalDateTime createdAt
) {
}
