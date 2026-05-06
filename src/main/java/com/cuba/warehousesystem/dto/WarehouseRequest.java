package com.cuba.warehousesystem.dto;

import jakarta.validation.constraints.NotBlank;

public record WarehouseRequest(
        @NotBlank String code,
        @NotBlank String name,
        String address,
        Boolean isActive
) {
}
