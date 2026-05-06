package com.cuba.warehousesystem.dto;

import java.util.List;

public record SupplierStatsReport(List<SupplierStatItem> supplierStats) {
    public record SupplierStatItem(String supplierName, Long totalIncomingOps, Integer totalIncomingQty) {}
}
