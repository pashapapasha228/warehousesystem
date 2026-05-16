package com.cuba.warehousesystem.controller;

import com.cuba.warehousesystem.dto.EdiMappingConfigRequest;
import com.cuba.warehousesystem.dto.EdiMappingConfigResponse;
import com.cuba.warehousesystem.service.EdiMappingConfigService;
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
@RequestMapping("/api/edi/mappings")
@RequiredArgsConstructor
public class EdiMappingConfigController {

    private final EdiMappingConfigService ediMappingConfigService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<EdiMappingConfigResponse> create(@Valid @RequestBody EdiMappingConfigRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ediMappingConfigService.create(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STOREKEEPER')")
    public ResponseEntity<EdiMappingConfigResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ediMappingConfigService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STOREKEEPER')")
    public ResponseEntity<Page<EdiMappingConfigResponse>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long partnerId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(ediMappingConfigService.getAll(search, partnerId, pageable));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<EdiMappingConfigResponse> update(@PathVariable Long id, @Valid @RequestBody EdiMappingConfigRequest request) {
        return ResponseEntity.ok(ediMappingConfigService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ediMappingConfigService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
