package com.cuba.warehousesystem.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record EdiSimulationRequest(
        @NotNull Long partnerId,
        @NotNull Long warehouseId,
        String documentNumber,
        @NotEmpty List<@Valid Item> items
) {
    public record Item(
            Long mappingId,
            Long productId,
            String externalProductCode,
            @NotNull @Min(1) Integer quantity,
            String unitOfMeasure
    ) {
    }
}
