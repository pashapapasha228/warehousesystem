package com.cuba.warehousesystem.repository;

import com.cuba.warehousesystem.model.EdiMappingConfig;
import com.cuba.warehousesystem.model.EdiMessageType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface EdiMappingConfigRepository extends JpaRepository<EdiMappingConfig, Long> {
    @EntityGraph(attributePaths = {"partner", "internalProduct"})
    Page<EdiMappingConfig> findAll(Pageable pageable);

    Optional<EdiMappingConfig> findByPartner_IdAndMessageTypeAndExternalProductCodeAndIsActiveTrue(
            Long partnerId,
            EdiMessageType messageType,
            String externalProductCode
    );

    Optional<EdiMappingConfig> findByPartner_IdAndMessageTypeAndExternalProductCode(
            Long partnerId,
            EdiMessageType messageType,
            String externalProductCode
    );

    List<EdiMappingConfig> findAllByPartner_IdAndMessageTypeAndExternalProductCode(
            Long partnerId,
            EdiMessageType messageType,
            String externalProductCode
    );

    boolean existsByPartner_IdAndMessageTypeAndExternalProductCode(
            Long partnerId,
            EdiMessageType messageType,
            String externalProductCode
    );
}
