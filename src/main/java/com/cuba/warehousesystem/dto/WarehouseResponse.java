package com.cuba.warehousesystem.dto;

import java.time.LocalDateTime;

public record WarehouseResponse(
        Long id,
        String code,
        String name,
        String address,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
