package com.cuba.warehousesystem.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AuditReport(List<AuditItem> items) {
    public record AuditItem(
            Long id,
            String entityName,
            String entityId,
            String action,
            String username,
            LocalDateTime occurredAt,
            String detailsJson
    ) {
    }
}
