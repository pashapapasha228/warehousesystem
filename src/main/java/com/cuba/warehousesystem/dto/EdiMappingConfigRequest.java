package com.cuba.warehousesystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EdiMappingConfigRequest(
        @NotNull Long partnerId,
        @NotBlank String externalProductCode,
        @NotNull Long internalProductId,
        Boolean isActive
) {
}
