package com.cuba.warehousesystem.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.envers.Audited;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "storage_cells",
        uniqueConstraints = @UniqueConstraint(name = "uk_storage_cells_warehouse_code", columnNames = {"warehouse_id", "code"})
)
@Audited
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorageCell {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Warehouse warehouse;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(length = 50)
    private String zone;

    @Column(length = 50)
    private String rack;

    @Column(length = 50)
    private String shelf;

    @Column(length = 50)
    private String level;

    @Column(name = "capacity_units", nullable = false)
    private Integer capacityUnits = 0;

    @Column(name = "max_weight_kg", nullable = false, precision = 12, scale = 3)
    private BigDecimal maxWeightKg = BigDecimal.ZERO;

    @Column(name = "max_volume_cm3", nullable = false, precision = 14, scale = 3)
    private BigDecimal maxVolumeCm3 = BigDecimal.ZERO;

    @Column(name = "length_cm", nullable = false, precision = 10, scale = 2)
    private BigDecimal lengthCm = BigDecimal.ZERO;

    @Column(name = "width_cm", nullable = false, precision = 10, scale = 2)
    private BigDecimal widthCm = BigDecimal.ZERO;

    @Column(name = "height_cm", nullable = false, precision = 10, scale = 2)
    private BigDecimal heightCm = BigDecimal.ZERO;

    @Column(name = "current_weight_kg", nullable = false, precision = 12, scale = 3)
    private BigDecimal currentWeightKg = BigDecimal.ZERO;

    @Column(name = "current_volume_cm3", nullable = false, precision = 14, scale = 3)
    private BigDecimal currentVolumeCm3 = BigDecimal.ZERO;

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
        if (capacityUnits == null) {
            capacityUnits = 0;
        }
        if (maxWeightKg == null) {
            maxWeightKg = BigDecimal.ZERO;
        }
        if (maxVolumeCm3 == null) {
            maxVolumeCm3 = BigDecimal.ZERO;
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
        if (currentWeightKg == null) {
            currentWeightKg = BigDecimal.ZERO;
        }
        if (currentVolumeCm3 == null) {
            currentVolumeCm3 = BigDecimal.ZERO;
        }
        if (isActive == null) {
            isActive = true;
        }
    }
}
