package com.cuba.warehousesystem.dto;

import java.util.Map;

public record EdiStatisticsReport(
        long totalMessages,
        Map<String, Long> messagesByStatus,
        Map<String, Long> messagesByType,
        long pendingQueueItems,
        long failedQueueItems
) {
}
