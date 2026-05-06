package com.cuba.warehousesystem.dto;

import java.time.LocalDateTime;

public record EdiPartnerResponse(
        Long id,
        String code,
        String name,
        String gln,
        Long counterpartyId,
        String counterpartyName,
        Long defaultWarehouseId,
        String defaultWarehouseCode,
        Boolean inboundEnabled,
        Boolean outboundEnabled,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
