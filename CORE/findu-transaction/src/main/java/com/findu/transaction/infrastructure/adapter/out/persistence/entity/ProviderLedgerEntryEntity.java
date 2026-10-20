package com.findu.transaction.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "provider_ledger_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderLedgerEntryEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Column(name = "transaction_id", length = 36)
    private String transactionId;

    @Column(name = "entry_type", length = 64, nullable = false)
    private String entryType;

    @Column(name = "direction", length = 16, nullable = false)
    private String direction;

    @Column(name = "amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "payable_balance_after", precision = 19, scale = 2, nullable = false)
    private BigDecimal payableBalanceAfter;

    @Column(name = "receivable_balance_after", precision = 19, scale = 2, nullable = false)
    private BigDecimal receivableBalanceAfter;

    @Column(name = "reference", length = 128, nullable = false)
    private String reference;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
