package com.cuba.warehousesystem.dto;

import java.math.BigDecimal;
import java.util.List;

public record CellUtilizationReport(List<CellUtilizationItem> utilizations) {
    public record CellUtilizationItem(
            String cellCode,
            BigDecimal currentVolume,
            BigDecimal maxVolume,
            BigDecimal currentWeight,
            BigDecimal maxWeight
    ) {}
}
