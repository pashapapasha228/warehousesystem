package com.cuba.warehousesystem.dto;

import java.util.List;

public record TopProductReport(List<TopProductItem> topProducts) {
    public record TopProductItem(String productSku, String productName, Integer totalQuantity, Long turnoverCount) {}
}
