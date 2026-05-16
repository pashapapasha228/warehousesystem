package com.cuba.warehousesystem.repository;

import com.cuba.warehousesystem.model.EdiPartner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EdiPartnerRepository extends JpaRepository<EdiPartner, Long> {
    Optional<EdiPartner> findByCode(String code);

    boolean existsByCode(String code);

    @EntityGraph(attributePaths = {"counterparty", "defaultWarehouse"})
    @Query("""
            select p from EdiPartner p
            left join p.counterparty c
            left join p.defaultWarehouse w
            where lower(p.code) like lower(concat('%', :search, '%'))
               or lower(p.name) like lower(concat('%', :search, '%'))
               or lower(coalesce(p.gln, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(c.name, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(c.code, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(w.code, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(w.name, '')) like lower(concat('%', :search, '%'))
            """)
    Page<EdiPartner> search(@Param("search") String search, Pageable pageable);
}
