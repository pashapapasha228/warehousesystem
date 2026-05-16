package com.cuba.warehousesystem.repository;

import com.cuba.warehousesystem.model.EdiProcessingQueue;
import com.cuba.warehousesystem.model.EdiMessageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EdiProcessingQueueRepository extends JpaRepository<EdiProcessingQueue, Long> {
    boolean existsByEdiMessage_Id(Long ediMessageId);

    Optional<EdiProcessingQueue> findByEdiMessage_Id(Long ediMessageId);

    long countByEdiMessage_Status(EdiMessageStatus status);

    @EntityGraph(attributePaths = {"ediMessage", "ediMessage.partner", "ediMessage.relatedOperation"})
    Page<EdiProcessingQueue> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"ediMessage", "ediMessage.partner", "ediMessage.relatedOperation"})
    Page<EdiProcessingQueue> findByEdiMessage_Status(EdiMessageStatus status, Pageable pageable);
}
