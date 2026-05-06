package com.cuba.warehousesystem.repository;

import com.cuba.warehousesystem.model.EdiPartner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EdiPartnerRepository extends JpaRepository<EdiPartner, Long> {
    Optional<EdiPartner> findByCode(String code);

    boolean existsByCode(String code);
}
