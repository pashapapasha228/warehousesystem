package com.cuba.warehousesystem.repository;

import com.cuba.warehousesystem.model.Warehouse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    Optional<Warehouse> findByCode(String code);

    long countByIsActiveTrue();

    @Query("""
            select w from Warehouse w
            where lower(w.code) like lower(concat('%', :search, '%'))
               or lower(w.name) like lower(concat('%', :search, '%'))
               or lower(coalesce(w.address, '')) like lower(concat('%', :search, '%'))
            """)
    Page<Warehouse> search(@Param("search") String search, Pageable pageable);
}
