package com.cuba.warehousesystem.dto;

import com.cuba.warehousesystem.model.OperationSource;
import com.cuba.warehousesystem.model.OperationType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record OperationRequest(
        @NotNull OperationType type,
        @NotNull Long warehouseId,
        Long counterpartyId,
        OperationSource source,
        String externalDocumentNumber,
        LocalDate documentDate,
        String comment,
        @NotEmpty List<@Valid ItemRequest> items
) {
    public record ItemRequest(
            @NotNull Long productId,
            @NotNull @Min(1) Integer quantity,
            BigDecimal unitPrice,
            Long fromCellId,
            Long toCellId
    ) {
    }
}
