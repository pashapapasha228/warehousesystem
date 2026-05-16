package com.cuba.warehousesystem.repository;

import com.cuba.warehousesystem.model.StockBalance;
import com.cuba.warehousesystem.model.StockBalanceId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockBalanceRepository extends JpaRepository<StockBalance, StockBalanceId> {
    @Override
    @EntityGraph(attributePaths = {"product", "cell", "cell.warehouse"})
    List<StockBalance> findAll();

    @EntityGraph(attributePaths = {"product", "cell", "cell.warehouse"})
    Optional<StockBalance> findByProduct_IdAndCell_Id(Long productId, Long cellId);

    @EntityGraph(attributePaths = {"product", "cell", "cell.warehouse"})
    List<StockBalance> findByProduct_Id(Long productId);

    @EntityGraph(attributePaths = {"product", "cell", "cell.warehouse"})
    List<StockBalance> findByProduct_IdAndCell_Warehouse_Id(Long productId, Long warehouseId);

    @EntityGraph(attributePaths = {"product", "cell", "cell.warehouse"})
    List<StockBalance> findByCell_Id(Long cellId);

    @EntityGraph(attributePaths = {"product", "cell", "cell.warehouse"})
    List<StockBalance> findByCell_Warehouse_Id(Long warehouseId);

    @EntityGraph(attributePaths = {"product", "cell", "cell.warehouse"})
    Page<StockBalance> findByCell_Warehouse_Id(Long warehouseId, Pageable pageable);

    @Query("""
            select coalesce(sum(sb.quantity - sb.reservedQuantity), 0)
            from StockBalance sb
            where sb.product.id = :productId and sb.cell.warehouse.id = :warehouseId
            """)
    Long sumAvailableByProductAndWarehouse(Long productId, Long warehouseId);

    @Override
    @EntityGraph(attributePaths = {"product", "cell", "cell.warehouse"})
    Page<StockBalance> findAll(Pageable pageable);
}
