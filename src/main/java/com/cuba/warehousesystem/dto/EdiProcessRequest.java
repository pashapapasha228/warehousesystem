package com.cuba.warehousesystem.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record EdiProcessRequest(
        List<@Valid CellAssignment> cellAssignments
) {
    public record CellAssignment(
            @NotNull @Min(0) Integer itemIndex,
            @NotNull Long cellId,
            @NotNull @Min(1) Integer quantity
    ) {
    }
}
