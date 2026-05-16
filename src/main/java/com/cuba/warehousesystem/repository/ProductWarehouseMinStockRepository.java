package com.cuba.warehousesystem.repository;

import com.cuba.warehousesystem.model.ProductWarehouseMinStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductWarehouseMinStockRepository extends JpaRepository<ProductWarehouseMinStock, Long> {
    List<ProductWarehouseMinStock> findByProduct_Id(Long productId);

    Optional<ProductWarehouseMinStock> findByProduct_IdAndWarehouse_Id(Long productId, Long warehouseId);
}
