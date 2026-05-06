package com.cuba.warehousesystem.event;

import com.cuba.warehousesystem.model.EdiMessageType;

import java.time.LocalDateTime;

public record EdiMessageReceivedEvent(
        Long ediMessageId,
        EdiMessageType messageType,
        Long partnerId,
        String messageRef,
        LocalDateTime occurredAt
) {
}
