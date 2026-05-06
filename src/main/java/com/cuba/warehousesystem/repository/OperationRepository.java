package com.cuba.warehousesystem.repository;

import com.cuba.warehousesystem.model.Counterparty;
import com.cuba.warehousesystem.model.Operation;
import com.cuba.warehousesystem.model.OperationStatus;
import com.cuba.warehousesystem.model.OperationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OperationRepository extends JpaRepository<Operation, Long> {
    @EntityGraph(attributePaths = {
            "warehouse",
            "createdBy",
            "completedBy",
            "counterparty",
            "items",
            "items.product",
            "items.fromCell",
            "items.toCell"
    })
    @Query("select o from Operation o where o.id = :id")
    Optional<Operation> findWithItemsById(Long id);

    @EntityGraph(attributePaths = {"warehouse", "createdBy", "completedBy", "counterparty"})
    Page<Operation> findByTypeAndStatus(OperationType type, OperationStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"warehouse", "createdBy", "completedBy", "counterparty"})
    Page<Operation> findByType(OperationType type, Pageable pageable);

    @EntityGraph(attributePaths = {"warehouse", "createdBy", "completedBy", "counterparty"})
    Page<Operation> findByStatus(OperationStatus status, Pageable pageable);

    @Query("SELECT o FROM Operation o WHERE o.status = 'COMPLETED' AND o.createdAt BETWEEN :start AND :end AND o.type = :type")
    List<Operation> findCompletedOperationsByTypeAndPeriod(LocalDateTime start, LocalDateTime end, OperationType type);

    // --- НОВЫЙ метод для аналитики по поставщикам ---
    @Query("SELECT o FROM Operation o WHERE o.status = 'COMPLETED' AND o.createdAt BETWEEN :start AND :end AND o.type = :type AND o.counterparty = :counterparty")
    List<Operation> findCompletedOperationsByTypePeriodAndCounterparty(
            LocalDateTime start, LocalDateTime end, OperationType type, Counterparty counterparty);

    // Общий метод для получения операций по периоду и статусу (может пригодиться)
    List<Operation> findByCreatedAtBetweenAndStatus(LocalDateTime start, LocalDateTime end, OperationStatus status);
}
