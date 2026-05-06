package com.cuba.warehousesystem.dto;

import com.cuba.warehousesystem.model.UserRole;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        String fullName,
        String email,
        UserRole role,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
