package com.cuba.warehousesystem.controller;

import com.cuba.warehousesystem.dto.StorageCellRequest;
import com.cuba.warehousesystem.dto.StorageCellResponse;
import com.cuba.warehousesystem.service.StorageCellService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/storage-cells")
@RequiredArgsConstructor
public class StorageCellController {

    private final StorageCellService storageCellService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<StorageCellResponse> create(@Valid @RequestBody StorageCellRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(storageCellService.create(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STOREKEEPER')")
    public ResponseEntity<StorageCellResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(storageCellService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STOREKEEPER')")
    public ResponseEntity<Page<StorageCellResponse>> getAll(
            @RequestParam(required = false) Long warehouseId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(storageCellService.getAll(warehouseId, pageable));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<StorageCellResponse> update(@PathVariable Long id, @Valid @RequestBody StorageCellRequest request) {
        return ResponseEntity.ok(storageCellService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        storageCellService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
