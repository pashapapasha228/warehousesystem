package com.cuba.warehousesystem.dto;

import java.util.List;

public record StockBalanceReport(List<BalanceItem> balances) {
    public record BalanceItem(String productSku, String productName, Integer quantity, String cellCode) {}
}
