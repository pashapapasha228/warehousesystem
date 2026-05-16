package com.cuba.warehousesystem.repository;

import com.cuba.warehousesystem.model.Counterparty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CounterpartyRepository extends JpaRepository<Counterparty, Long> {
    Optional<Counterparty> findByCode(String code);

    @Query("""
            select c from Counterparty c
            where lower(c.code) like lower(concat('%', :search, '%'))
               or lower(c.name) like lower(concat('%', :search, '%'))
               or lower(coalesce(c.taxId, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(c.gln, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(c.email, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(c.phone, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(c.address, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(c.contactInfo, '')) like lower(concat('%', :search, '%'))
            """)
    Page<Counterparty> search(@Param("search") String search, Pageable pageable);
}
