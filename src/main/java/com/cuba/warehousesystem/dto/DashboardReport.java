package com.cuba.warehousesystem.dto;

import com.cuba.warehousesystem.model.EdiMessageStatus;
import com.cuba.warehousesystem.model.EdiMessageType;
import com.cuba.warehousesystem.model.OperationStatus;
import com.cuba.warehousesystem.model.OperationType;

import java.time.LocalDateTime;
import java.util.List;

public record DashboardReport(
        DashboardKpi kpi,
        List<AttentionItem> attentionItems,
        List<WorkQueueItem> receivingQueue,
        List<WorkQueueItem> shippingQueue,
        EdiSummary ediSummary,
        CellUtilizationSummary cellUtilization,
        List<StockWarningItem> stockWarnings,
        List<RecentOperationItem> recentOperations
) {
    public DashboardReport(
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
            List<ProductAlert> lowStockAlerts,
            DashboardActionBlock toReceive,
            DashboardActionBlock toShip,
            DashboardActionBlock stuckEdi,
            DashboardActionBlock lowStock,
            DashboardActionBlock overloadedCells,
            DashboardActionBlock waitingOperations,
            TodaySummary todaySummary,
            List<OperationActionItem> inboundOperations,
            List<OperationActionItem> outboundOperations,
            List<OperationActionItem> waitingOperationItems,
            List<EdiActionItem> stuckEdiMessages,
            List<CellLoadItem> overloadedCellItems,
            List<ActivityItem> todayActivity
    ) {
        this(
                new DashboardKpi(0, 0, pendingEdiMessages, failedEdiMessages, 0, lowStockProducts, 0, 0, completedOperations, draftOperations),
                List.of(),
                List.of(),
                List.of(),
                new EdiSummary(0, pendingEdiMessages, 0, failedEdiMessages, List.of()),
                new CellUtilizationSummary(0, 0, 0, overloadedCellItems.size(), overloadedCellItems),
                lowStockAlerts.stream()
                        .map(alert -> new StockWarningItem(alert.productName(), alert.sku(), alert.warehouseId(), alert.warehouseCode(), alert.currentStock(), alert.minLevel(), alert.currentStock() == 0 ? "Нет остатка" : "Ниже минимума", "/products"))
                        .toList(),
                List.of()
        );
    }

    public record DashboardActionBlock(
            String title,
            String subtitle,
            long count,
            String severity,
            String actionLabel,
            String actionUrl
    ) {
    }

    public record OperationActionItem(
            Long id,
            String operationNumber,
            OperationType type,
            OperationStatus status,
            String warehouseCode,
            String counterpartyName,
            Integer itemCount,
            Integer totalQuantity,
            LocalDateTime createdAt,
            String actionUrl
    ) {
    }

    public record EdiActionItem(
            Long id,
            EdiMessageType messageType,
            EdiMessageStatus ediStatus,
            String documentNumber,
            String partnerCode,
            String errorMessage,
            LocalDateTime receivedAt,
            String actionUrl
    ) {
    }

    public record TodaySummary(
            long newOperations,
            long completedOperations,
            long receivedEdiMessages
    ) {
    }

    public record ActivityItem(
            String entityName,
            String entityId,
            String action,
            String username,
            LocalDateTime occurredAt,
            String actionUrl
    ) {
    }

    public record DashboardKpi(
            long expectedReceiving,
            long readyToShip,
            long pendingEdi,
            long failedEdi,
            long zeroStockProducts,
            long belowMinProducts,
            double averageVolumeUtilization,
            double averageWeightUtilization,
            long completedOperations,
            long draftOperations
    ) {
    }

    public record AttentionItem(
            String type,
            String severity,
            int priority,
            String title,
            String detail,
            String actionLabel,
            String actionUrl
    ) {
    }

    public record WorkQueueItem(
            Long id,
            String source,
            String partnerName,
            String documentNumber,
            Integer itemCount,
            String status,
            EdiMessageType messageType,
            OperationType operationType,
            String actionLabel,
            String actionUrl
    ) {
    }

    public record EdiSummary(
            long receivedToday,
            long queued,
            long processed,
            long failed,
            List<EdiProblemItem> problemMessages
    ) {
    }

    public record EdiProblemItem(
            Long id,
            EdiMessageType messageType,
            EdiMessageStatus status,
            String partnerName,
            String errorMessage,
            String actionUrl
    ) {
    }

    public record CellUtilizationSummary(
            double averageVolumePercent,
            double averageWeightPercent,
            long cellsAbove80Percent,
            long cellsAbove90Percent,
            List<CellLoadItem> topCells
    ) {
    }

    public record CellLoadItem(
            Long id,
            String warehouseCode,
            String cellCode,
            Double volumePercent,
            Double weightPercent,
            String actionUrl
    ) {
        public CellLoadItem(String cellCode, Double volumePercent, Double weightPercent, String actionUrl) {
            this(null, null, cellCode, volumePercent, weightPercent, actionUrl);
        }
    }

    public record StockWarningItem(
            String productName,
            String sku,
            Long warehouseId,
            String warehouseCode,
            Integer currentStock,
            Integer minLevel,
            String status,
            String actionUrl
    ) {
    }

    public record RecentOperationItem(
            Long id,
            LocalDateTime occurredAt,
            OperationType type,
            OperationStatus status,
            String operationNumber,
            String warehouseCode,
            String username,
            String actionUrl
    ) {
    }
}
