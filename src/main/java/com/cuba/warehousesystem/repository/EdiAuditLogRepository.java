package com.cuba.warehousesystem.repository;

import com.cuba.warehousesystem.model.EdiAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EdiAuditLogRepository extends JpaRepository<EdiAuditLog, Long> {
    @EntityGraph(attributePaths = {"ediMessage"})
    Page<EdiAuditLog> findByEdiMessage_Id(Long ediMessageId, Pageable pageable);
}
