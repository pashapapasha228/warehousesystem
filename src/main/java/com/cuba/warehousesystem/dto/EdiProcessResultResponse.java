package com.cuba.warehousesystem.dto;

import com.cuba.warehousesystem.model.EdiMessageStatus;

public record EdiProcessResultResponse(
        Long queueItemId,
        Long ediMessageId,
        EdiMessageStatus messageStatus,
        Long relatedOperationId,
        String errorMessage
) {
}
