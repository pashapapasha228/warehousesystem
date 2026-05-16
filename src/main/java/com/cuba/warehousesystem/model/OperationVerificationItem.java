package com.cuba.warehousesystem.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "operation_verification_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperationVerificationItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "verification_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private OperationVerification verification;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operation_item_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private OperationItem operationItem;

    @Column(name = "planned_quantity", nullable = false)
    private Integer plannedQuantity;

    @Column(name = "actual_quantity", nullable = false)
    private Integer actualQuantity;

    @Column(name = "discrepancy_quantity", nullable = false)
    private Integer discrepancyQuantity;

    @Column(length = 255)
    private String reason;
}
