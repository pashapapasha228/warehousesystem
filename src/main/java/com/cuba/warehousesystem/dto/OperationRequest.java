package com.cuba.warehousesystem.dto;

import java.util.List;

public record OperationRequest(
        String type, // INCOME, OUTCOME, MOVE
        Long warehouseId,
        // --- НОВОЕ: ID контрагента ---
        Long counterpartyId, // Может быть null для внутренних операций
        // ----------------------------
        List<ItemRequest> items
) {
    public record ItemRequest(Long productId, Integer quantity, Long toCellId, Long fromCellId) {}
}
