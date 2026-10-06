package com.findu.transaction.domain.model.balance;

import com.findu.transaction.domain.valueobject.Money;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
public class DailyBalance {

    private String id;
    private Long providerId;
    private LocalDate balanceDate;

    private Money openingBalance;
    private Money totalEarnings;
    private Money totalCommissions;
    private Money totalTips;
    private Money totalExtras;
    private Money cashPayments;
    private Money digitalPayments;
    private Money providerDebt;
    private Money pendingPayout;
    private Money closingBalance;

    private Instant createdAt;

    private DailyBalance() {}

    public static DailyBalance create(
            Long providerId,
            LocalDate balanceDate,
            Money openingBalance,
            Money totalEarnings,
            Money totalCommissions,
            Money totalTips,
            Money totalExtras,
            Money cashPayments,
            Money digitalPayments,
            Money providerDebt,
            Money pendingPayout,
            Money closingBalance) {

        if (providerId == null) throw new IllegalArgumentException("El providerId es obligatorio");
        if (balanceDate == null) throw new IllegalArgumentException("La fecha de cierre es obligatoria");

        DailyBalance b = new DailyBalance();
        b.id = "BAL-" + providerId + "-" + balanceDate;
        b.providerId = providerId;
        b.balanceDate = balanceDate;
        b.openingBalance = openingBalance != null ? openingBalance : Money.ZERO;
        b.totalEarnings = totalEarnings != null ? totalEarnings : Money.ZERO;
        b.totalCommissions = totalCommissions != null ? totalCommissions : Money.ZERO;
        b.totalTips = totalTips != null ? totalTips : Money.ZERO;
        b.totalExtras = totalExtras != null ? totalExtras : Money.ZERO;
        b.cashPayments = cashPayments != null ? cashPayments : Money.ZERO;
        b.digitalPayments = digitalPayments != null ? digitalPayments : Money.ZERO;
        b.providerDebt = providerDebt != null ? providerDebt : Money.ZERO;
        b.pendingPayout = pendingPayout != null ? pendingPayout : Money.ZERO;
        b.closingBalance = closingBalance != null ? closingBalance : Money.ZERO;
        b.createdAt = Instant.now();

        return b;
    }

    public static DailyBalance reconstitute(
            String id,
            Long providerId,
            LocalDate balanceDate,
            Money openingBalance,
            Money totalEarnings,
            Money totalCommissions,
            Money totalTips,
            Money totalExtras,
            Money cashPayments,
            Money digitalPayments,
            Money providerDebt,
            Money pendingPayout,
            Money closingBalance,
            Instant createdAt) {

        DailyBalance b = new DailyBalance();
        b.id = id;
        b.providerId = providerId;
        b.balanceDate = balanceDate;
        b.openingBalance = openingBalance;
        b.totalEarnings = totalEarnings;
        b.totalCommissions = totalCommissions;
        b.totalTips = totalTips;
        b.totalExtras = totalExtras;
        b.cashPayments = cashPayments;
        b.digitalPayments = digitalPayments;
        b.providerDebt = providerDebt;
        b.pendingPayout = pendingPayout;
        b.closingBalance = closingBalance;
        b.createdAt = createdAt;
        return b;
    }
}
