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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.envers.Audited;

import java.math.BigDecimal;

@Entity
@Table(name = "operation_items")
@Audited
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperationItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operation_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Operation operation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "unit_of_measure", nullable = false, length = 20)
    private String unitOfMeasure = "pcs";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_cell_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private StorageCell fromCell;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_cell_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private StorageCell toCell;

    @PrePersist
    @PreUpdate
    void normalizeDefaults() {
        if (unitPrice == null) {
            unitPrice = BigDecimal.ZERO;
        }
        if (unitOfMeasure == null || unitOfMeasure.isBlank()) {
            unitOfMeasure = product != null && product.getUnitOfMeasure() != null ? product.getUnitOfMeasure() : "pcs";
        }
    }
}
