package com.cuba.warehousesystem.dto;

import com.cuba.warehousesystem.model.CounterpartyType;

import java.time.LocalDateTime;

public record CounterpartyResponse(
        Long id,
        String code,
        String name,
        CounterpartyType type,
        String taxId,
        String gln,
        String email,
        String phone,
        String address,
        String contactInfo,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
