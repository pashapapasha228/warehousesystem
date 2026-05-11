package com.cuba.warehousesystem.dto;

import com.cuba.warehousesystem.model.UserRole;

public record CurrentUserResponse(
        Long id,
        String username,
        String fullName,
        String email,
        UserRole role,
        Boolean isActive
) {
}
