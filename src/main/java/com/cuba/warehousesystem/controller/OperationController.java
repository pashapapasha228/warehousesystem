package com.cuba.warehousesystem.controller;

import com.cuba.warehousesystem.dto.OperationItemResponse;
import com.cuba.warehousesystem.dto.OperationRequest;
import com.cuba.warehousesystem.dto.OperationResponse;
import com.cuba.warehousesystem.model.Operation;
import com.cuba.warehousesystem.service.OperationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/operations")
@RequiredArgsConstructor
public class OperationController {

    private final OperationService operationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_STOREKEEPER')")
    public ResponseEntity<OperationResponse> createDraft(@RequestBody OperationRequest request, Authentication authentication) {
        Operation op = operationService.createDraftOperation(request, authentication.getName());
        return ResponseEntity.ok(mapToResponse(op));
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<OperationResponse> completeOperation(@PathVariable Long id, Authentication authentication) {
        Operation op = operationService.completeOperation(id, authentication.getName());
        return ResponseEntity.ok(mapToResponse(op));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_STOREKEEPER')")
    public ResponseEntity<List<OperationResponse>> getAllOperations() {
        // В реальном проекте здесь будет сервисный вызов
        // Для простоты диплома можно вернуть пустой список или добавить логику
        return ResponseEntity.ok(List.of()); // TODO: Implement service call
    }

    private OperationResponse mapToResponse(Operation op) {
        List<OperationItemResponse> itemResponses = op.getItems().stream()
                .map(item -> new OperationItemResponse(
                        item.getId(),
                        item.getProduct().getId(),
                        item.getQuantity(),
                        item.getFromCell() != null ? item.getFromCell().getId() : null,
                        item.getToCell() != null ? item.getToCell().getId() : null
                ))
                .collect(Collectors.toList());

        return new OperationResponse(
                op.getId(),
                op.getOperationNumber(),
                op.getType(),
                op.getStatus(),
                op.getWarehouse().getId(),
                op.getCreatedBy().getId(),
                op.getCreatedAt(),
                op.getCompletedAt(),
                itemResponses
        );
    }
}
