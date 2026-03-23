package com.cuba.warehousesystem.dto;

public record ProductAlert(String productName, String sku, Integer currentStock, Integer minLevel) {}
