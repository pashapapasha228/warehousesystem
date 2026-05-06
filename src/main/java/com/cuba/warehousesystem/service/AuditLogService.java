package com.cuba.warehousesystem.service;

import com.cuba.warehousesystem.model.AuditLog;
import com.cuba.warehousesystem.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(String entityName, String entityId, String action, String username, String detailsJson) {
        AuditLog auditLog = new AuditLog();
        auditLog.setEntityName(entityName);
        auditLog.setEntityId(entityId);
        auditLog.setAction(action);
        auditLog.setUsername(username == null || username.isBlank() ? "system" : username);
        auditLog.setOccurredAt(LocalDateTime.now());
        auditLog.setDetailsJson(detailsJson);
        auditLogRepository.save(auditLog);
    }
}
