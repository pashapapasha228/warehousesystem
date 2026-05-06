package com.cuba.warehousesystem.repository;

import com.cuba.warehousesystem.model.StockBalance;
import com.cuba.warehousesystem.model.StockBalanceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockBalanceRepository extends JpaRepository<StockBalance, StockBalanceId> {
    Optional<StockBalance> findByProduct_IdAndCell_Id(Long productId, Long cellId);

    List<StockBalance> findByProduct_Id(Long productId);

    List<StockBalance> findByCell_Id(Long cellId);

    List<StockBalance> findByCell_Warehouse_Id(Long warehouseId);
}
