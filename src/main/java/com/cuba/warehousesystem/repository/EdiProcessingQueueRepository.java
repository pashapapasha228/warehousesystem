package com.cuba.warehousesystem.repository;

import com.cuba.warehousesystem.model.EdiProcessingQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EdiProcessingQueueRepository extends JpaRepository<EdiProcessingQueue, Long> {
    boolean existsByEdiMessage_Id(Long ediMessageId);
}
