package com.cuba.warehousesystem.dto;

import java.time.LocalDateTime;

public record LogoutResponse(
        String message,
        LocalDateTime loggedOutAt
) {
}
