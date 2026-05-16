package com.cuba.warehousesystem.repository;

import com.cuba.warehousesystem.model.StorageCell;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    long countByIsActiveTrue();
}
