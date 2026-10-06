package com.findu.transaction.domain.model.settlement;

import com.findu.transaction.domain.enums.ProviderType;
import com.findu.transaction.domain.enums.SettlementStatus;
import com.findu.transaction.domain.enums.SettlementType;
import com.findu.transaction.domain.valueobject.Money;
import lombok.Getter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public class Settlement {

    private String id;
    private String settlementCode;
    private Long providerId;
    private ProviderType providerType;

    private Money grossAmount;         // Payout bruto acumulado
    private Money debtCompensationAmount; // Descuento de compensación de deuda
    private Money netAmount;           // Payout neto a pagar

    private Instant periodStart;
    private Instant periodEnd;

    private SettlementStatus status;
    private SettlementType settlementType;

    private Instant scheduledAt;
    private Instant executedAt;
    private Instant createdAt;
    private Instant updatedAt;

    private List<String> dailyBalanceIds = new ArrayList<>();

    private Settlement() {}

    public static Settlement create(
            Long providerId,
            ProviderType providerType,
            SettlementType settlementType,
            Money grossAmount,
            Money debtCompensationAmount,
            Instant periodStart,
            Instant periodEnd,
            List<String> dailyBalanceIds) {

        if (providerId == null) throw new IllegalArgumentException("El providerId es obligatorio");
        if (grossAmount == null) throw new IllegalArgumentException("El monto bruto es obligatorio");

        Money compensation = debtCompensationAmount != null ? debtCompensationAmount : Money.ZERO;
        Money net = grossAmount.subtract(compensation);
        if (!net.isGreaterThanOrEqual(Money.ZERO)) {
            net = Money.ZERO;
        }

        Settlement settlement = new Settlement();
        settlement.id = UUID.randomUUID().toString();
        settlement.settlementCode = "SET-" + Instant.now().toEpochMilli() + "-" + providerId;
        settlement.providerId = providerId;
        settlement.providerType = providerType != null ? providerType : ProviderType.PERSONA_NATURAL;
        settlement.settlementType = settlementType != null ? settlementType : SettlementType.DAILY;
        settlement.grossAmount = grossAmount;
        settlement.debtCompensationAmount = compensation;
        settlement.netAmount = net;
        settlement.periodStart = periodStart;
        settlement.periodEnd = periodEnd;
        settlement.status = SettlementStatus.CREATED;
        settlement.createdAt = Instant.now();
        settlement.updatedAt = Instant.now();
        if (dailyBalanceIds != null) {
            settlement.dailyBalanceIds = new ArrayList<>(dailyBalanceIds);
        }

        return settlement;
    }

    public static Settlement create(
            Long providerId,
            ProviderType providerType,
            Money grossAmount,
            Money debtCompensationAmount,
            Instant periodStart,
            Instant periodEnd) {
        return create(providerId, providerType, SettlementType.DAILY, grossAmount, debtCompensationAmount, periodStart, periodEnd, null);
    }

    public static Settlement reconstitute(
            String id,
            String settlementCode,
            Long providerId,
            ProviderType providerType,
            Money grossAmount,
            Money debtCompensationAmount,
            Money netAmount,
            Instant periodStart,
            Instant periodEnd,
            SettlementStatus status,
            SettlementType settlementType,
            Instant scheduledAt,
            Instant executedAt,
            Instant createdAt,
            Instant updatedAt,
            List<String> dailyBalanceIds) {

        Settlement s = new Settlement();
        s.id = id;
        s.settlementCode = settlementCode;
        s.providerId = providerId;
        s.providerType = providerType;
        s.grossAmount = grossAmount;
        s.debtCompensationAmount = debtCompensationAmount;
        s.netAmount = netAmount;
        s.periodStart = periodStart;
        s.periodEnd = periodEnd;
        s.status = status != null ? status : SettlementStatus.CREATED;
        s.settlementType = settlementType != null ? settlementType : SettlementType.DAILY;
        s.scheduledAt = scheduledAt;
        s.executedAt = executedAt;
        s.createdAt = createdAt;
        s.updatedAt = updatedAt != null ? updatedAt : createdAt;
        if (dailyBalanceIds != null) {
            s.dailyBalanceIds = new ArrayList<>(dailyBalanceIds);
        }
        return s;
    }

    public static Settlement reconstitute(
            String id,
            String settlementCode,
            Long providerId,
            ProviderType providerType,
            Money grossAmount,
            Money debtCompensationAmount,
            Money netAmount,
            Instant periodStart,
            Instant periodEnd,
            Instant createdAt) {

        return reconstitute(id, settlementCode, providerId, providerType, grossAmount, debtCompensationAmount, netAmount,
                periodStart, periodEnd, SettlementStatus.CREATED, SettlementType.DAILY, null, null, createdAt, createdAt, null);
    }

    public void markAsRestricted() {
        this.status = SettlementStatus.RESTRICTED;
        this.updatedAt = Instant.now();
    }

    public void markAsSkipped() {
        this.status = SettlementStatus.SKIPPED;
        this.updatedAt = Instant.now();
    }
}
