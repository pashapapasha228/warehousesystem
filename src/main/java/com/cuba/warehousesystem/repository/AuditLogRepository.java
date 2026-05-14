package com.cuba.warehousesystem.repository;

import com.cuba.warehousesystem.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findByOccurredAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    boolean existsByEntityNameAndEntityIdAndAction(String entityName, String entityId, String action);
}
