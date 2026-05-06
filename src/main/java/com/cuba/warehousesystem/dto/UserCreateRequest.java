package com.cuba.warehousesystem.dto;

import com.cuba.warehousesystem.model.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserCreateRequest(
        @NotBlank String username,
        @NotBlank String password,
        String fullName,
        String email,
        @NotNull UserRole role,
        Boolean isActive
) {
}
