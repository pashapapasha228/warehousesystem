package com.cuba.warehousesystem.dto;

public record EdiCustomerReceiptRequest(
        Long messageId,
        Long operationId,
        String documentNumber
) {
}
