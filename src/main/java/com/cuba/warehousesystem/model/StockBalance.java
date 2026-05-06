package com.cuba.warehousesystem.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.envers.Audited;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_balances")
@Audited
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(StockBalanceId.class)
public class StockBalance {
    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    @ToString.Exclude
    private Product product;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cell_id", nullable = false)
    @ToString.Exclude
    private StorageCell cell;

    @Column(nullable = false)
    private Integer quantity = 0;

    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity = 0;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public StockBalance(Product product, StorageCell cell, Integer quantity) {
        this.product = product;
        this.cell = cell;
        this.quantity = quantity;
        this.reservedQuantity = 0;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        normalizeDefaults();
        updatedAt = LocalDateTime.now();
    }

    private void normalizeDefaults() {
        if (quantity == null) {
            quantity = 0;
        }
        if (reservedQuantity == null) {
            reservedQuantity = 0;
        }
    }
}
