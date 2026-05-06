package com.cuba.warehousesystem.dto;

import com.cuba.warehousesystem.model.EdiMessageStatus;
import com.cuba.warehousesystem.model.EdiQueueStatus;

public record EdiProcessResultResponse(
        Long queueItemId,
        Long ediMessageId,
        EdiQueueStatus queueStatus,
        EdiMessageStatus messageStatus,
        Long relatedOperationId,
        String errorMessage
) {
}
