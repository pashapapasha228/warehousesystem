package com.cuba.warehousesystem.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record EdiPartnerRequest(
        @NotBlank String code,
        @NotBlank String name,
        String gln,
        Long counterpartyId,
        List<Long> warehouseIds,
        Boolean inboundEnabled,
        Boolean outboundEnabled,
        Boolean isActive
) {
}
