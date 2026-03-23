package com.cuba.warehousesystem.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String sku;

    @Column(nullable = false)
    private String name;

    private String category;

    @Column(name = "unit_of_measure", length = 20)
    private String unitOfMeasure = "шт";

    // --- Новые поля с BigDecimal ---
    @Column(name = "weight_per_unit_kg", precision = 8, scale = 3) // Теперь работает
    private BigDecimal weightPerUnitKg;

    @Column(name = "volume_per_unit_cubic_cm", precision = 10, scale = 3) // Теперь работает
    private BigDecimal volumePerUnitCubicCm;

    @Column(name = "length_per_unit_cm", precision = 8, scale = 2) // Теперь работает
    private BigDecimal lengthPerUnitCm;

    @Column(name = "width_per_unit_cm", precision = 8, scale = 2) // Теперь работает
    private BigDecimal widthPerUnitCm;
    @Column(name = "height_per_unit_cm", precision = 8, scale = 2) // Теперь работает
    private BigDecimal heightPerUnitCm;
}
