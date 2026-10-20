package com.findu.transaction.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "settlement_daily_balances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementDailyBalanceEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "settlement_id", length = 36, nullable = false)
    private String settlementId;

    @Column(name = "daily_balance_id", length = 36, nullable = false)
    private String dailyBalanceId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
