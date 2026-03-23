package com.cuba.warehousesystem.repository;

import com.cuba.warehousesystem.model.Operation;
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
}
