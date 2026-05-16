package com.cuba.warehousesystem.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "operation_verifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperationVerification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operation_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Operation operation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VerificationDecision decision;

    @Column(length = 255)
    private String comment;

    @Column(name = "verified_by", nullable = false, length = 100)
    private String verifiedBy;

    @Column(name = "verified_at", nullable = false)
    private LocalDateTime verifiedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "verification", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<OperationVerificationItem> items = new ArrayList<>();
}
