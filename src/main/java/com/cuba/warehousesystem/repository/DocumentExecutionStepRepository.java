package com.cuba.warehousesystem.repository;

import com.cuba.warehousesystem.model.DocumentExecutionStep;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentExecutionStepRepository extends JpaRepository<DocumentExecutionStep, Long> {
    @EntityGraph(attributePaths = {"operation", "ediMessage"})
    List<DocumentExecutionStep> findByOperation_IdOrderByCreatedAtAsc(Long operationId);

    @EntityGraph(attributePaths = {"operation", "ediMessage"})
    List<DocumentExecutionStep> findByEdiMessage_IdOrderByCreatedAtAsc(Long ediMessageId);
}
