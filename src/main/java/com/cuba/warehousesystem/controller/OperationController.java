package com.cuba.warehousesystem.controller;

import com.cuba.warehousesystem.dto.DocumentExecutionStepResponse;
import com.cuba.warehousesystem.dto.OperationRequest;
import com.cuba.warehousesystem.dto.OperationResponse;
import com.cuba.warehousesystem.dto.OperationVerificationRequest;
import com.cuba.warehousesystem.dto.OperationVerificationResponse;
import com.cuba.warehousesystem.dto.StockBalanceResponse;
import com.cuba.warehousesystem.model.OperationStatus;
import com.cuba.warehousesystem.model.OperationType;
import com.cuba.warehousesystem.service.OperationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/operations")
@RequiredArgsConstructor
public class OperationController {

    private final OperationService operationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STOREKEEPER')")
    public ResponseEntity<OperationResponse> createDraft(
            @Valid @RequestBody OperationRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(operationService.createDraftOperation(request, authentication.getName()));
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<OperationResponse> completeOperation(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(operationService.completeOperation(id, authentication.getName()));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<OperationResponse> cancelOperation(@PathVariable Long id) {
        return ResponseEntity.ok(operationService.cancelOperation(id));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STOREKEEPER')")
    public ResponseEntity<OperationResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(operationService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STOREKEEPER')")
    public ResponseEntity<Page<OperationResponse>> getAllOperations(
            @RequestParam(required = false) OperationType type,
            @RequestParam(required = false) OperationStatus status,
            @RequestParam(required = false) Long warehouseId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(operationService.getAll(type, status, warehouseId, pageable));
    }

    @GetMapping("/stock-balances")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STOREKEEPER')")
    public ResponseEntity<Page<StockBalanceResponse>> getStockBalance(
            @RequestParam(required = false) Long warehouseId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(operationService.getStockBalance(warehouseId, pageable));
    }

    @PostMapping("/{id}/verification")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STOREKEEPER')")
    public ResponseEntity<OperationVerificationResponse> verifyOperation(
            @PathVariable Long id,
            @Valid @RequestBody OperationVerificationRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(operationService.verifyOperation(id, request, authentication.getName()));
    }

    @GetMapping("/{id}/verification")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STOREKEEPER')")
    public ResponseEntity<List<OperationVerificationResponse>> getVerifications(@PathVariable Long id) {
        return ResponseEntity.ok(operationService.getVerifications(id));
    }

    @GetMapping("/{id}/execution-chain")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STOREKEEPER')")
    public ResponseEntity<List<DocumentExecutionStepResponse>> getExecutionChain(@PathVariable Long id) {
        return ResponseEntity.ok(operationService.getExecutionChain(id));
    }
}
