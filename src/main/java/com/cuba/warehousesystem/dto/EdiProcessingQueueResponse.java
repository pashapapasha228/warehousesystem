package com.cuba.warehousesystem.dto;

import com.cuba.warehousesystem.model.EdiQueueStatus;

import java.time.LocalDateTime;

public record EdiProcessingQueueResponse(
        Long id,
        Long ediMessageId,
        String messageRef,
        String partnerCode,
        EdiQueueStatus status,
        Integer attemptCount,
        LocalDateTime scheduledAt,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        String lastError
) {
}
