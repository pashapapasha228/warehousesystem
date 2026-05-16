package com.cuba.warehousesystem.dto;

import com.cuba.warehousesystem.model.VerificationDecision;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OperationVerificationRequest(
        @NotNull VerificationDecision decision,
        String comment,
        @NotEmpty List<@Valid Item> items
) {
    public record Item(
            @NotNull Long operationItemId,
            @NotNull @Min(0) Integer actualQuantity,
            String reason
    ) {
    }
}
