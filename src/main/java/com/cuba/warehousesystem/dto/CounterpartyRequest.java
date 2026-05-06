package com.cuba.warehousesystem.dto;

import com.cuba.warehousesystem.model.CounterpartyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CounterpartyRequest(
        @NotBlank String code,
        @NotBlank String name,
        @NotNull CounterpartyType type,
        String taxId,
        String gln,
        String email,
        String phone,
        String address,
        String contactInfo,
        Boolean isActive
) {
}
