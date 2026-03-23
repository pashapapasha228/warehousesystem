package com.cuba.warehousesystem.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Entity
@Table(name = "stock_balances")
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(StockBalanceId.class)
public class StockBalance {
    @Id
    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @Id
    @ManyToOne
    @JoinColumn(name = "cell_id")
    private StorageCell cell;

    private Integer quantity = 0;
}
