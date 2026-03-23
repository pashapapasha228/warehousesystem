package com.cuba.warehousesystem.repository;

import com.cuba.warehousesystem.model.StorageCell;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StorageCellRepository extends JpaRepository<StorageCell, Long> {}
