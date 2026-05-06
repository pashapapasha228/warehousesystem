package com.cuba.warehousesystem.dto;

import com.cuba.warehousesystem.model.EdiMessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EdiMappingConfigRequest(
        @NotNull Long partnerId,
        @NotNull EdiMessageType messageType,
        @NotBlank String externalProductCode,
        String externalUom,
        @NotNull Long internalProductId,
        String internalUom,
        Boolean isActive
) {
}
