package com.cuba.warehousesystem.repository;

import com.cuba.warehousesystem.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    long countByIsActiveTrue();

    java.util.List<Product> findByNameContainingIgnoreCase(String name);

    @Query("""
            select p from Product p
            where lower(p.sku) like lower(concat('%', :search, '%'))
               or lower(coalesce(p.barcode, '')) like lower(concat('%', :search, '%'))
               or lower(p.name) like lower(concat('%', :search, '%'))
            """)
    Page<Product> search(@Param("search") String search, Pageable pageable);
}
