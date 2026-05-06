package com.cuba.warehousesystem.repository;

import com.cuba.warehousesystem.model.EdiMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EdiMessageRepository extends JpaRepository<EdiMessage, Long> {
}
