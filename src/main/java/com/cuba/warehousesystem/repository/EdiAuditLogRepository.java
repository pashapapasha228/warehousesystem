package com.cuba.warehousesystem.repository;

import com.cuba.warehousesystem.model.EdiAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EdiAuditLogRepository extends JpaRepository<EdiAuditLog, Long> {
}
