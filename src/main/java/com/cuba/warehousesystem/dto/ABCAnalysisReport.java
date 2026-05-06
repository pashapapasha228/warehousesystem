package com.cuba.warehousesystem.dto;

import java.util.List;

public record ABCAnalysisReport(List<ABCItem> analysis) {
    public record ABCItem(
            String productSku,
            String productName,
            Integer totalQuantity,
            Double percentage,
            String category // A, B, C
    ) {}
}
