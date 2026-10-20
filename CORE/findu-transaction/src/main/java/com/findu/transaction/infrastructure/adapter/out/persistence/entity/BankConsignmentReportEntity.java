package com.findu.transaction.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "bank_consignment_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankConsignmentReportEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Column(name = "amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "reference", length = 128, nullable = false)
    private String reference;

    @Column(name = "bank_name", length = 64, nullable = false)
    private String bankName;

    @Column(name = "status", length = 32, nullable = false)
    private String status;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "confirmed_by", length = 128)
    private String confirmedBy;

    @Column(name = "notes", length = 255)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
