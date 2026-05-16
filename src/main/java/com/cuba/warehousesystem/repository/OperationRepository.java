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

    @EntityGraph(attributePaths = {"warehouse", "createdBy", "completedBy", "counterparty"})
    Page<Operation> findByWarehouse_Id(Long warehouseId, Pageable pageable);

    @EntityGraph(attributePaths = {"warehouse", "createdBy", "completedBy", "counterparty"})
    Page<Operation> findByWarehouse_IdAndType(Long warehouseId, OperationType type, Pageable pageable);

    @EntityGraph(attributePaths = {"warehouse", "createdBy", "completedBy", "counterparty"})
    Page<Operation> findByWarehouse_IdAndStatus(Long warehouseId, OperationStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"warehouse", "createdBy", "completedBy", "counterparty"})
    Page<Operation> findByWarehouse_IdAndTypeAndStatus(Long warehouseId, OperationType type, OperationStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"warehouse", "createdBy", "completedBy", "counterparty", "items", "items.product", "items.fromCell", "items.toCell"})
    @Query("""
            select distinct o from Operation o
            join o.items i
            where i.product.id = :productId
            and (:warehouseId is null or o.warehouse.id = :warehouseId)
            order by o.createdAt desc
            """)
    List<Operation> findRecentByProduct(Long productId, Long warehouseId, Pageable pageable);

    @EntityGraph(attributePaths = {"warehouse", "counterparty", "items", "items.product", "items.fromCell", "items.toCell"})
    @Query("SELECT o FROM Operation o WHERE o.status = 'COMPLETED' AND o.createdAt BETWEEN :start AND :end AND o.type = :type")
    List<Operation> findCompletedOperationsByTypeAndPeriod(LocalDateTime start, LocalDateTime end, OperationType type);

    // --- НОВЫЙ метод для аналитики по поставщикам ---
    @EntityGraph(attributePaths = {"counterparty", "items", "items.product"})
    @Query("SELECT o FROM Operation o WHERE o.status = 'COMPLETED' AND o.createdAt BETWEEN :start AND :end AND o.type = :type AND o.counterparty = :counterparty")
    List<Operation> findCompletedOperationsByTypePeriodAndCounterparty(
            LocalDateTime start, LocalDateTime end, OperationType type, Counterparty counterparty);

    // Общий метод для получения операций по периоду и статусу (может пригодиться)
    @EntityGraph(attributePaths = {"warehouse", "counterparty", "items", "items.product", "items.fromCell", "items.toCell"})
    List<Operation> findByCreatedAtBetweenAndStatus(LocalDateTime start, LocalDateTime end, OperationStatus status);

    long countByStatus(OperationStatus status);

    boolean existsByOperationNumber(String operationNumber);
}
