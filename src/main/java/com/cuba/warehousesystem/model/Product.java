package com.cuba.warehousesystem.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.envers.Audited;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Audited
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 100)
    private String sku;

    @Column(unique = true, length = 100)
    private String barcode;

    @Column(nullable = false)
    private String name;

    @Column(length = 100)
    private String category;

    @Column(name = "unit_of_measure", nullable = false, length = 20)
    private String unitOfMeasure = "pcs";

    @Column(name = "min_stock_level", nullable = false)
    private Integer minStockLevel = 0;

    @Column(name = "weight_per_unit_kg", nullable = false, precision = 12, scale = 3)
    private BigDecimal weightPerUnitKg = BigDecimal.ZERO;

    @Column(name = "volume_per_unit_cm3", nullable = false, precision = 14, scale = 3)
    private BigDecimal volumePerUnitCm3 = BigDecimal.ZERO;

    @Column(name = "length_cm", nullable = false, precision = 10, scale = 2)
    private BigDecimal lengthCm = BigDecimal.ZERO;

    @Column(name = "width_cm", nullable = false, precision = 10, scale = 2)
    private BigDecimal widthCm = BigDecimal.ZERO;

    @Column(name = "height_cm", nullable = false, precision = 10, scale = 2)
    private BigDecimal heightCm = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        normalizeDefaults();
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        normalizeDefaults();
        updatedAt = LocalDateTime.now();
    }

    private void normalizeDefaults() {
        if (unitOfMeasure == null || unitOfMeasure.isBlank()) {
            unitOfMeasure = "pcs";
        }
        if (minStockLevel == null) {
            minStockLevel = 0;
        }
        if (weightPerUnitKg == null) {
            weightPerUnitKg = BigDecimal.ZERO;
        }
        if (volumePerUnitCm3 == null) {
            volumePerUnitCm3 = BigDecimal.ZERO;
        }
        if (lengthCm == null) {
            lengthCm = BigDecimal.ZERO;
        }
        if (widthCm == null) {
            widthCm = BigDecimal.ZERO;
        }
        if (heightCm == null) {
            heightCm = BigDecimal.ZERO;
        }
        if (isActive == null) {
            isActive = true;
        }
    }
}
