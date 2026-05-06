package com.cuba.warehousesystem.dto;

import com.cuba.warehousesystem.model.EdiDirection;
import com.cuba.warehousesystem.model.EdiMessageStatus;
import com.cuba.warehousesystem.model.EdiMessageType;

import java.time.LocalDateTime;

public record EdiMessageResponse(
        Long id,
        EdiMessageType messageType,
        EdiDirection direction,
        EdiMessageStatus status,
        String interchangeRef,
        String messageRef,
        String documentNumber,
        String rawPayload,
        String normalizedPayload,
        Long partnerId,
        String partnerCode,
        Long relatedOperationId,
        String relatedOperationNumber,
        LocalDateTime receivedAt,
        LocalDateTime processedAt,
        String errorMessage
) {
}
