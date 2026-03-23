package com.cuba.warehousesystem.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "operation_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperationItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operation_id")
    private Operation operation;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    private Integer quantity;

    @ManyToOne
    @JoinColumn(name = "from_cell_id")
    private StorageCell fromCell;

    @ManyToOne
    @JoinColumn(name = "to_cell_id")
    private StorageCell toCell;
}
