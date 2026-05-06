package com.cuba.warehousesystem.repository;

import com.cuba.warehousesystem.model.Counterparty;
import com.cuba.warehousesystem.model.Operation;
import com.cuba.warehousesystem.model.OperationStatus;
import com.cuba.warehousesystem.model.OperationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OperationRepository extends JpaRepository<Operation, Long> {
    @Query("SELECT o FROM Operation o WHERE o.status = 'COMPLETED' AND o.createdAt BETWEEN :start AND :end AND o.type = :type")
    List<Operation> findCompletedOperationsByTypeAndPeriod(LocalDateTime start, LocalDateTime end, OperationType type);

    // --- НОВЫЙ метод для аналитики по поставщикам ---
    @Query("SELECT o FROM Operation o WHERE o.status = 'COMPLETED' AND o.createdAt BETWEEN :start AND :end AND o.type = :type AND o.counterparty = :counterparty")
    List<Operation> findCompletedOperationsByTypePeriodAndCounterparty(
            LocalDateTime start, LocalDateTime end, OperationType type, Counterparty counterparty);

    // Общий метод для получения операций по периоду и статусу (может пригодиться)
    List<Operation> findByCreatedAtBetweenAndStatus(LocalDateTime start, LocalDateTime end, OperationStatus status);
}
