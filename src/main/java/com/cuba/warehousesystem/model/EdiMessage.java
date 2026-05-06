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
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "edi_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EdiMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 30)
    private EdiMessageType messageType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EdiDirection direction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EdiMessageStatus status = EdiMessageStatus.RECEIVED;

    @Column(name = "interchange_ref", length = 100)
    private String interchangeRef;

    @Column(name = "message_ref", length = 100)
    private String messageRef;

    @Column(name = "document_number", length = 100)
    private String documentNumber;

    @Column(name = "raw_payload", columnDefinition = "text")
    private String rawPayload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "normalized_payload", columnDefinition = "jsonb")
    private String normalizedPayload;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id")
    private EdiPartner partner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_operation_id")
    private Operation relatedOperation;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt = LocalDateTime.now();

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;
}
