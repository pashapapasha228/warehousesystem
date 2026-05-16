package com.cuba.warehousesystem.repository;

import com.cuba.warehousesystem.model.EdiMessage;
import com.cuba.warehousesystem.model.EdiMessageStatus;
import com.cuba.warehousesystem.model.EdiMessageType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EdiMessageRepository extends JpaRepository<EdiMessage, Long> {
    long countByStatus(EdiMessageStatus status);

    long countByMessageType(EdiMessageType messageType);

    boolean existsByMessageRef(String messageRef);

    Optional<EdiMessage> findByMessageRef(String messageRef);

    Optional<EdiMessage> findByRelatedOperation_Id(Long operationId);

    @EntityGraph(attributePaths = {"partner", "relatedOperation"})
    Page<EdiMessage> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"partner", "relatedOperation"})
    Page<EdiMessage> findByMessageTypeAndStatus(EdiMessageType messageType, EdiMessageStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"partner", "relatedOperation"})
    Page<EdiMessage> findByMessageType(EdiMessageType messageType, Pageable pageable);

    @EntityGraph(attributePaths = {"partner", "relatedOperation"})
    Page<EdiMessage> findByStatus(EdiMessageStatus status, Pageable pageable);
}
