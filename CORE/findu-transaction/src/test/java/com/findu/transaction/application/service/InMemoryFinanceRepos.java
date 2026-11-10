package com.findu.transaction.application.service;

import com.findu.transaction.application.port.out.persistence.DailyBalanceRepository;
import com.findu.transaction.application.port.out.persistence.PayoutRepository;
import com.findu.transaction.application.port.out.persistence.ProviderAccountRepository;
import com.findu.transaction.application.port.out.persistence.SettlementRepository;
import com.findu.transaction.domain.enums.LedgerDirection;
import com.findu.transaction.domain.enums.LedgerEntryType;
import com.findu.transaction.domain.enums.PayoutStatus;
import com.findu.transaction.domain.enums.ProviderAccountStatus;
import com.findu.transaction.domain.enums.ProviderType;
import com.findu.transaction.domain.enums.SettlementStatus;
import com.findu.transaction.domain.enums.SettlementType;
import com.findu.transaction.domain.model.account.ProviderAccount;
import com.findu.transaction.domain.model.account.ProviderLedgerEntry;
import com.findu.transaction.domain.model.balance.DailyBalance;
import com.findu.transaction.domain.model.payout.Payout;
import com.findu.transaction.domain.model.settlement.Settlement;
import com.findu.transaction.domain.valueobject.Money;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Repositorios en memoria para probar liquidacion e idempotencia sin base de datos. */
public final class InMemoryFinanceRepos {

    private InMemoryFinanceRepos() {}

    public static final Instant DAY_26_OCT_A = Instant.parse("2026-10-26T15:00:00Z");
    public static final Instant DAY_26_OCT_B = Instant.parse("2026-10-26T18:00:00Z");

    /** Cuenta del ejemplo real: 2 ganancias digitales (99.000 + 87.300) y 2 comisiones en efectivo (800 + 500). */
    public static ProviderAccount exampleAccount(Long providerId) {
        List<ProviderLedgerEntry> entries = new ArrayList<>(List.of(
                entry(providerId, LedgerEntryType.SERVICE_EARNING, 99000, LedgerDirection.CREDIT, "TX-1", DAY_26_OCT_A),
                entry(providerId, LedgerEntryType.SERVICE_EARNING, 87300, LedgerDirection.CREDIT, "TX-2", DAY_26_OCT_B),
                entry(providerId, LedgerEntryType.CASH_PAYMENT_DEBT, 800, LedgerDirection.DEBIT, "TX-3", DAY_26_OCT_A),
                entry(providerId, LedgerEntryType.CASH_PAYMENT_DEBT, 500, LedgerDirection.DEBIT, "TX-4", DAY_26_OCT_B)));
        return account(providerId, 186300, 1300, entries);
    }

    public static ProviderAccount account(Long providerId, long payable, long receivable, List<ProviderLedgerEntry> entries) {
        return ProviderAccount.reconstitute("ACC-" + providerId, providerId, ProviderType.PERSONA_NATURAL,
                Money.of(payable), Money.of(receivable), Money.of(125000), ProviderAccountStatus.NORMAL,
                entries, DAY_26_OCT_A, DAY_26_OCT_A);
    }

    public static ProviderLedgerEntry entry(Long providerId, LedgerEntryType type, long amount, LedgerDirection dir,
                                            String ref, Instant at) {
        return new ProviderLedgerEntry("E-" + ref + "-" + type, providerId, type, Money.of(amount), dir, ref, "test", at);
    }

    public static DailyBalance dailyBalance(Long providerId, LocalDate date) {
        return DailyBalance.create(providerId, date, Money.ZERO, Money.of(198000), Money.of(22000), Money.ZERO,
                Money.ZERO, Money.of(13000), Money.of(186300), Money.of(1300), Money.of(186300), Money.of(185000));
    }

    public static class Accounts implements ProviderAccountRepository {
        public final Map<Long, ProviderAccount> store = new LinkedHashMap<>();
        public int saves;

        @Override public ProviderAccount save(ProviderAccount account) { saves++; store.put(account.getProviderId(), account); return account; }
        @Override public Optional<ProviderAccount> findByProviderId(Long providerId) { return Optional.ofNullable(store.get(providerId)); }
        @Override public List<ProviderAccount> findAll() { return new ArrayList<>(store.values()); }
    }

    public static class Settlements implements SettlementRepository {
        public final Map<String, Settlement> store = new LinkedHashMap<>();

        @Override public Settlement save(Settlement s) { store.put(s.getId(), s); return s; }
        @Override public Optional<Settlement> findById(String id) { return Optional.ofNullable(store.get(id)); }
        @Override public Optional<Settlement> findBySettlementCode(String code) {
            return store.values().stream().filter(s -> code.equals(s.getSettlementCode())).findFirst();
        }
        @Override public List<Settlement> findByProviderId(Long providerId) {
            return store.values().stream().filter(s -> providerId.equals(s.getProviderId())).collect(Collectors.toList());
        }
        @Override public Optional<Settlement> findByProviderIdAndPeriodStartAndPeriodEndAndSettlementType(
                Long providerId, Instant start, Instant end, SettlementType type) { return Optional.empty(); }
        @Override public List<String> findSettledDailyBalanceIdsByProviderId(Long providerId) { return List.of(); }
        @Override public Optional<Settlement> findLatestCompletedByProviderId(Long providerId) {
            return store.values().stream()
                    .filter(s -> providerId.equals(s.getProviderId()) && s.getStatus() == SettlementStatus.COMPLETED)
                    .max(Comparator.comparing(Settlement::getPeriodEnd));
        }
    }

    public static class Payouts implements PayoutRepository {
        public final Map<String, Payout> store = new LinkedHashMap<>();

        @Override public Payout save(Payout p) { store.put(p.getId(), p); return p; }
        @Override public Optional<Payout> findById(String id) { return Optional.ofNullable(store.get(id)); }
        @Override public Optional<Payout> findByPayoutCode(String code) {
            return store.values().stream().filter(p -> code.equals(p.getPayoutCode())).findFirst();
        }
        @Override public List<Payout> findByStatus(PayoutStatus status) {
            return store.values().stream().filter(p -> p.getStatus() == status).collect(Collectors.toList());
        }
        @Override public List<Payout> findByProviderId(Long providerId) {
            return store.values().stream().filter(p -> providerId.equals(p.getProviderId())).collect(Collectors.toList());
        }
        @Override public Optional<Payout> findBySettlementId(String settlementId) {
            return store.values().stream().filter(p -> settlementId.equals(p.getSettlementId())).findFirst();
        }
    }

    public static class DailyBalances implements DailyBalanceRepository {
        public final Map<String, DailyBalance> store = new LinkedHashMap<>();

        @Override public DailyBalance save(DailyBalance b) { store.put(b.getId(), b); return b; }
        @Override public Optional<DailyBalance> findByProviderIdAndBalanceDate(Long providerId, LocalDate date) {
            return store.values().stream()
                    .filter(b -> providerId.equals(b.getProviderId()) && date.equals(b.getBalanceDate())).findFirst();
        }
        @Override public List<DailyBalance> findByProviderId(Long providerId) {
            return store.values().stream().filter(b -> providerId.equals(b.getProviderId())).collect(Collectors.toList());
        }
        @Override public List<DailyBalance> findByProviderIdAndBalanceDateBetween(Long providerId, LocalDate s, LocalDate e) {
            return findByProviderId(providerId).stream()
                    .filter(b -> !b.getBalanceDate().isBefore(s) && !b.getBalanceDate().isAfter(e)).collect(Collectors.toList());
        }
    }
}
