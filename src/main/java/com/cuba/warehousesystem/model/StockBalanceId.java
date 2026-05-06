package com.cuba.warehousesystem.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockBalanceId implements Serializable {
    private Long product;
    private Long cell;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StockBalanceId)) return false;
        StockBalanceId that = (StockBalanceId) o;
        return Objects.equals(product, that.product) && Objects.equals(cell, that.cell);
    }

    @Override
    public int hashCode() {
        return Objects.hash(product, cell);
    }
}
