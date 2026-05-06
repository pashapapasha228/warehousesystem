package com.cuba.warehousesystem.repository;

import com.cuba.warehousesystem.model.EdiMappingConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EdiMappingConfigRepository extends JpaRepository<EdiMappingConfig, Long> {
}
