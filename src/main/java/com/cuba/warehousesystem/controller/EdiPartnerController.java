package com.cuba.warehousesystem.controller;

import com.cuba.warehousesystem.dto.EdiPartnerRequest;
import com.cuba.warehousesystem.dto.EdiPartnerResponse;
import com.cuba.warehousesystem.service.EdiPartnerService;
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
@RequestMapping("/api/edi/partners")
@RequiredArgsConstructor
public class EdiPartnerController {

    private final EdiPartnerService ediPartnerService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<EdiPartnerResponse> create(@Valid @RequestBody EdiPartnerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ediPartnerService.create(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STOREKEEPER')")
    public ResponseEntity<EdiPartnerResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ediPartnerService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STOREKEEPER')")
    public ResponseEntity<Page<EdiPartnerResponse>> getAll(
            @RequestParam(required = false) String search,
            Pageable pageable
    ) {
        return ResponseEntity.ok(ediPartnerService.getAll(search, pageable));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<EdiPartnerResponse> update(@PathVariable Long id, @Valid @RequestBody EdiPartnerRequest request) {
        return ResponseEntity.ok(ediPartnerService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ediPartnerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
