package com.cuba.warehousesystem.dto;

import java.util.List;

public record DashboardReport(
        long activeProducts,
        long activeWarehouses,
        long activeStorageCells,
        long completedOperations,
        long draftOperations,
        long totalStockQuantity,
        long lowStockProducts,
        long pendingEdiMessages,
        long failedEdiMessages,
        List<CellUtilizationReport.CellUtilizationItem> mostUtilizedCells,
        List<ProductAlert> lowStockAlerts
) {
}
