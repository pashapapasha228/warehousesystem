package com.cuba.warehousesystem.repository;

import com.cuba.warehousesystem.model.OperationVerification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OperationVerificationRepository extends JpaRepository<OperationVerification, Long> {
    @EntityGraph(attributePaths = {"items", "items.operationItem", "items.operationItem.product"})
    List<OperationVerification> findByOperation_IdOrderByVerifiedAtDesc(Long operationId);

    @EntityGraph(attributePaths = {"items", "items.operationItem", "items.operationItem.product"})
    Optional<OperationVerification> findFirstByOperation_IdOrderByVerifiedAtDesc(Long operationId);
}
