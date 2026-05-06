package com.cuba.warehousesystem.repository;

import com.cuba.warehousesystem.model.StorageCell;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StorageCellRepository extends JpaRepository<StorageCell, Long> {
    Optional<StorageCell> findByWarehouse_IdAndCode(Long warehouseId, String code);

    List<StorageCell> findByWarehouse_Id(Long warehouseId);
}
