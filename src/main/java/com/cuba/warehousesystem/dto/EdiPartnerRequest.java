package com.cuba.warehousesystem.dto;

import jakarta.validation.constraints.NotBlank;

public record EdiPartnerRequest(
        @NotBlank String code,
        @NotBlank String name,
        String gln,
        Long counterpartyId,
        Long defaultWarehouseId,
        Boolean inboundEnabled,
        Boolean outboundEnabled,
        Boolean isActive
) {
}
