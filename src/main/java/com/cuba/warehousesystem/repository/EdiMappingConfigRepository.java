package com.cuba.warehousesystem.repository;

import com.cuba.warehousesystem.model.EdiMappingConfig;
import com.cuba.warehousesystem.model.EdiMessageType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface EdiMappingConfigRepository extends JpaRepository<EdiMappingConfig, Long> {
    @EntityGraph(attributePaths = {"partner", "internalProduct"})
    Page<EdiMappingConfig> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"partner", "internalProduct"})
    @Query("""
            select m from EdiMappingConfig m
            join m.partner p
            join m.internalProduct product
            where lower(m.externalProductCode) like lower(concat('%', :search, '%'))
               or lower(p.code) like lower(concat('%', :search, '%'))
               or lower(p.name) like lower(concat('%', :search, '%'))
               or lower(product.sku) like lower(concat('%', :search, '%'))
               or lower(product.name) like lower(concat('%', :search, '%'))
            """)
    Page<EdiMappingConfig> search(@Param("search") String search, Pageable pageable);

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

    @EntityGraph(attributePaths = {"partner", "internalProduct"})
    List<EdiMappingConfig> findByPartner_IdAndMessageTypeAndIsActiveTrue(Long partnerId, EdiMessageType messageType);

    boolean existsByPartner_IdAndMessageTypeAndExternalProductCode(
            Long partnerId,
            EdiMessageType messageType,
            String externalProductCode
    );
}
