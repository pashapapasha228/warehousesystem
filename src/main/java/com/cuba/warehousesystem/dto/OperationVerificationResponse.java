package com.cuba.warehousesystem.dto;

import com.cuba.warehousesystem.model.VerificationDecision;

import java.time.LocalDateTime;
import java.util.List;

public record OperationVerificationResponse(
        Long id,
        Long operationId,
        VerificationDecision decision,
        String comment,
        String verifiedBy,
        LocalDateTime verifiedAt,
        List<OperationVerificationItemResponse> items
) {
}
