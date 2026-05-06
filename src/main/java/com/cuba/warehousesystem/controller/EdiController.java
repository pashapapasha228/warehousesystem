package com.cuba.warehousesystem.controller;

import com.cuba.warehousesystem.dto.EdiAuditLogResponse;
import com.cuba.warehousesystem.dto.EdiMessageReceiveRequest;
import com.cuba.warehousesystem.dto.EdiMessageResponse;
import com.cuba.warehousesystem.dto.EdiProcessResultResponse;
import com.cuba.warehousesystem.dto.EdiProcessingQueueResponse;
import com.cuba.warehousesystem.model.EdiMessageStatus;
import com.cuba.warehousesystem.model.EdiMessageType;
import com.cuba.warehousesystem.model.EdiQueueStatus;
import com.cuba.warehousesystem.service.EdiProcessingService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/edi")
@RequiredArgsConstructor
public class EdiController {

    private final EdiProcessingService ediProcessingService;

    @PostMapping("/messages/inbound")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<EdiMessageResponse> receiveInbound(@Valid @RequestBody EdiMessageReceiveRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ediProcessingService.receiveInbound(request));
    }

    @GetMapping("/messages/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STOREKEEPER')")
    public ResponseEntity<EdiMessageResponse> getMessage(@PathVariable Long id) {
        return ResponseEntity.ok(ediProcessingService.getMessage(id));
    }

    @GetMapping("/messages")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STOREKEEPER')")
    public ResponseEntity<Page<EdiMessageResponse>> getMessages(
            @RequestParam(required = false) EdiMessageType type,
            @RequestParam(required = false) EdiMessageStatus status,
            Pageable pageable
    ) {
        return ResponseEntity.ok(ediProcessingService.getMessages(type, status, pageable));
    }

    @GetMapping("/queue")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STOREKEEPER')")
    public ResponseEntity<Page<EdiProcessingQueueResponse>> getQueue(
            @RequestParam(required = false) EdiQueueStatus status,
            Pageable pageable
    ) {
        return ResponseEntity.ok(ediProcessingService.getQueue(status, pageable));
    }

    @PostMapping("/queue/{id}/process")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<EdiProcessResultResponse> processQueueItem(@PathVariable Long id, Authentication authentication) {
        String username = authentication == null ? "system" : authentication.getName();
        return ResponseEntity.ok(ediProcessingService.processQueueItem(id, username));
    }

    @GetMapping("/audit")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Page<EdiAuditLogResponse>> getAudit(
            @RequestParam(required = false) Long messageId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(ediProcessingService.getAudit(messageId, pageable));
    }
}
