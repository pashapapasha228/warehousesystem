package com.cuba.warehousesystem.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record EdiSimulationRequest(
        @NotNull Long partnerId,
        @NotNull Long warehouseId,
        @NotNull Long productId,
        @NotNull @Min(1) Integer quantity,
        Long cellId,
        String documentNumber
) {
}
