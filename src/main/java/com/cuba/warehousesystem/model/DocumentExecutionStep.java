package com.cuba.warehousesystem.model;

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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "document_execution_steps")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentExecutionStep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operation_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Operation operation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "edi_message_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private EdiMessage ediMessage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private DocumentExecutionStage stage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DocumentExecutionStatus status = DocumentExecutionStatus.DONE;

    @Column(length = 255)
    private String details;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
