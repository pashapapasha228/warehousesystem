package com.cuba.warehousesystem.dto;

import com.cuba.warehousesystem.model.EdiQueueStatus;
import com.cuba.warehousesystem.model.EdiMessageStatus;
import com.cuba.warehousesystem.model.EdiMessageType;

import java.time.LocalDateTime;

public record EdiProcessingQueueResponse(
        Long id,
        Long ediMessageId,
        EdiMessageType messageType,
        EdiMessageStatus messageStatus,
        String messageRef,
        String documentNumber,
        String partnerCode,
        Long relatedOperationId,
        String normalizedPayload,
        EdiQueueStatus status,
        Integer attemptCount,
        LocalDateTime scheduledAt,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        String lastError
) {
}
