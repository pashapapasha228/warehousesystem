package com.cuba.warehousesystem.dto;

import com.cuba.warehousesystem.model.UserRole;
import jakarta.validation.constraints.NotNull;

public record UserUpdateRequest(
        String password,
        String fullName,
        String email,
        @NotNull UserRole role,
        Boolean isActive
) {
}
