package com.cuba.warehousesystem.dto;

import com.cuba.warehousesystem.model.EdiMessageType;

import java.time.LocalDateTime;

public record EdiMappingConfigResponse(
        Long id,
        Long partnerId,
        String partnerCode,
        EdiMessageType messageType,
        String externalProductCode,
        String externalUom,
        Long internalProductId,
        String internalSku,
        String internalUom,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
