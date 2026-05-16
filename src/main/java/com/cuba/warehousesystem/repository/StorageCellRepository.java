package com.cuba.warehousesystem.repository;

import com.cuba.warehousesystem.model.StorageCell;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StorageCellRepository extends JpaRepository<StorageCell, Long> {
    @Override
    @EntityGraph(attributePaths = {"warehouse"})
    List<StorageCell> findAll();

    Optional<StorageCell> findByWarehouse_IdAndCode(Long warehouseId, String code);

    @EntityGraph(attributePaths = {"warehouse"})
    List<StorageCell> findByWarehouse_Id(Long warehouseId);

    @EntityGraph(attributePaths = {"warehouse"})
    Page<StorageCell> findByWarehouse_Id(Long warehouseId, Pageable pageable);

    @EntityGraph(attributePaths = {"warehouse"})
    @Query("""
            select c from StorageCell c
            where (:warehouseId is null or c.warehouse.id = :warehouseId)
              and (
                   lower(c.code) like lower(concat('%', :search, '%'))
                or lower(c.warehouse.code) like lower(concat('%', :search, '%'))
                or lower(c.warehouse.name) like lower(concat('%', :search, '%'))
                or lower(coalesce(c.zone, '')) like lower(concat('%', :search, '%'))
                or lower(coalesce(c.rack, '')) like lower(concat('%', :search, '%'))
                or lower(coalesce(c.shelf, '')) like lower(concat('%', :search, '%'))
                or lower(coalesce(c.level, '')) like lower(concat('%', :search, '%'))
              )
            """)
    Page<StorageCell> search(@Param("warehouseId") Long warehouseId, @Param("search") String search, Pageable pageable);

    long countByIsActiveTrue();
}
