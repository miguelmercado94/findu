package com.findu.transaction.domain.model.account;

import com.findu.transaction.domain.enums.LedgerEntryType;
import com.findu.transaction.domain.valueobject.Money;
import com.findu.transaction.application.service.InMemoryFinanceRepos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderAccountCompensationTest {

    @Test
    @DisplayName("applyCompensation(max) compensa min(payable, receivable, max) y asienta COMPENSATION_ADJUSTMENT")
    void compensationIsCappedByMax() {
        ProviderAccount account = InMemoryFinanceRepos.account(1L, 10000, 5000, List.of());

        Money applied = account.applyCompensation(Money.of(2000));

        assertThat(applied.getAmount()).isEqualByComparingTo("2000");
        assertThat(account.getPayableBalance().getAmount()).isEqualByComparingTo("8000");
        assertThat(account.getReceivableBalance().getAmount()).isEqualByComparingTo("3000");
        assertThat(account.getLedgerEntries()).singleElement()
                .satisfies(e -> assertThat(e.getType()).isEqualTo(LedgerEntryType.COMPENSATION_ADJUSTMENT));
    }

    @Test
    @DisplayName("applyCompensation() sin argumentos conserva su comportamiento (min(payable, receivable))")
    void legacyCompensationUnchanged() {
        ProviderAccount account = InMemoryFinanceRepos.account(1L, 1000, 5000, List.of());

        Money applied = account.applyCompensation();

        assertThat(applied.getAmount()).isEqualByComparingTo("1000");
        assertThat(account.getPayableBalance().getAmount()).isEqualByComparingTo("0");
        assertThat(account.getReceivableBalance().getAmount()).isEqualByComparingTo("4000");
    }

    @Test
    @DisplayName("Sin saldo o max 0 no compensa nada")
    void nothingToCompensate() {
        ProviderAccount account = InMemoryFinanceRepos.account(1L, 0, 5000, List.of());
        assertThat(account.applyCompensation(Money.of(100)).getAmount()).isEqualByComparingTo("0");
        assertThat(InMemoryFinanceRepos.account(1L, 100, 100, List.of()).applyCompensation(Money.ZERO).getAmount())
                .isEqualByComparingTo("0");
    }
}