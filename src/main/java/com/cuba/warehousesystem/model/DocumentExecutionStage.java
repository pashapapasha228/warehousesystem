package com.cuba.warehousesystem.model;

public enum DocumentExecutionStage {
    EDI_RECEIVED,
    DRAFT_CREATED,
    FACT_CHECK,
    STOCK_POSTED,
    COMPLETED,
    CANCELLED
}
