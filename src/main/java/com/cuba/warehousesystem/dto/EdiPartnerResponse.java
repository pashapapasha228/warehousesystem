package com.cuba.warehousesystem.dto;

import com.cuba.warehousesystem.model.CounterpartyType;

import java.time.LocalDateTime;
import java.util.List;

public record EdiPartnerResponse(
        Long id,
        String code,
        String name,
        String gln,
        Long counterpartyId,
        String counterpartyName,
        CounterpartyType counterpartyType,
        List<Long> warehouseIds,
        List<EdiPartnerWarehouseResponse> warehouses,
        Boolean inboundEnabled,
        Boolean outboundEnabled,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
