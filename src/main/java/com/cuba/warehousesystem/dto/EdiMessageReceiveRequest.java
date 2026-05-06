package com.cuba.warehousesystem.dto;

import com.cuba.warehousesystem.model.EdiMessageType;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

public record EdiMessageReceiveRequest(
        Long partnerId,
        String partnerCode,
        @NotNull EdiMessageType messageType,
        String interchangeRef,
        String messageRef,
        String documentNumber,
        String rawPayload,
        @NotNull JsonNode normalizedPayload
) {
}
