package com.cuba.warehousesystem.event;

import com.cuba.warehousesystem.model.EdiMessageStatus;
import com.cuba.warehousesystem.model.EdiMessageType;

import java.time.LocalDateTime;

public record EdiMessageProcessedEvent(
        Long ediMessageId,
        EdiMessageType messageType,
        EdiMessageStatus status,
        Long relatedOperationId,
        String errorMessage,
        LocalDateTime occurredAt
) {
}
