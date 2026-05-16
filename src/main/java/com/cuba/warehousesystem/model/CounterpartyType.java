package com.cuba.warehousesystem.model;

public enum CounterpartyType {
    SUPPLIER,
    CUSTOMER,
    BOTH;

    public boolean canActAsSupplier() {
        return this == SUPPLIER || this == BOTH;
    }

    public boolean canActAsCustomer() {
        return this == CUSTOMER || this == BOTH;
    }
}
