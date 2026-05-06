package com.cuba.warehousesystem.repository;

import com.cuba.warehousesystem.model.EdiProcessingQueue;
import com.cuba.warehousesystem.model.EdiQueueStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EdiProcessingQueueRepository extends JpaRepository<EdiProcessingQueue, Long> {
    boolean existsByEdiMessage_Id(Long ediMessageId);

    long countByStatus(EdiQueueStatus status);

    @EntityGraph(attributePaths = {"ediMessage", "ediMessage.partner"})
    Page<EdiProcessingQueue> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"ediMessage", "ediMessage.partner"})
    Page<EdiProcessingQueue> findByStatus(EdiQueueStatus status, Pageable pageable);
}
