package com.cuba.warehousesystem.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "storage_cells")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorageCell {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @Column(nullable = false)
    private String code; // e.g., A-01-05

    private Integer capacity = 100; // Количество мест (например, паллет)

    // --- Новые поля с BigDecimal ---
    @Column(name = "max_weight_kg", precision = 10, scale = 2) // Теперь работает
    private BigDecimal maxWeightKg;

    @Column(name = "length_cm", precision = 8, scale = 2) // Теперь работает
    private BigDecimal lengthCm;

    @Column(name = "width_cm", precision = 8, scale = 2) // Теперь работает
    private BigDecimal widthCm;

    @Column(name = "height_cm", precision = 8, scale = 2) // Теперь работает
    private BigDecimal heightCm;

    @Column(name = "current_weight_kg", precision = 10, scale = 2) // Теперь работает
    private BigDecimal currentWeightKg;

    @Column(name = "current_volume_cubic_cm", precision = 12, scale = 2) // Теперь работает
    private BigDecimal currentVolumeCubicCm;

    // Метод для вычисления объёма ячейки
    public BigDecimal getCalculatedVolumeCubicCm() {
        if (lengthCm != null && widthCm != null && heightCm != null) {
            return lengthCm.multiply(widthCm).multiply(heightCm);
        }
        return BigDecimal.ZERO;
    }
}
