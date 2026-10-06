package com.findu.transaction.application.service;

import com.findu.transaction.application.port.in.GetProviderBalanceUseCase;
import com.findu.transaction.application.port.in.GetProviderDebtUseCase;
import com.findu.transaction.application.port.in.GetProviderLedgerUseCase;
import com.findu.transaction.application.port.in.query.GetProviderBalanceQuery;
import com.findu.transaction.application.port.in.query.GetProviderDebtQuery;
import com.findu.transaction.application.port.in.query.GetProviderLedgerQuery;
import com.findu.transaction.application.port.out.persistence.ProviderAccountRepository;
import com.findu.transaction.domain.enums.ProviderType;
import com.findu.transaction.domain.model.account.ProviderAccount;
import com.findu.transaction.domain.model.account.ProviderLedgerEntry;
import com.findu.transaction.domain.valueobject.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GetProviderAccountService implements GetProviderBalanceUseCase, GetProviderLedgerUseCase, GetProviderDebtUseCase {

    private final ProviderAccountRepository providerAccountRepository;

    @Override
    public Optional<ProviderAccount> execute(GetProviderBalanceQuery query) {
        if (query == null || query.getProviderId() == null) {
            return Optional.empty();
        }
        return providerAccountRepository.findByProviderId(query.getProviderId());
    }

    @Override
    public List<ProviderLedgerEntry> execute(GetProviderLedgerQuery query) {
        if (query == null || query.getProviderId() == null) {
            return List.of();
        }
        Optional<ProviderAccount> account = providerAccountRepository.findByProviderId(query.getProviderId());
        if (account.isEmpty()) {
            return List.of();
        }
        List<ProviderLedgerEntry> entries = account.get().getLedgerEntries();
        if (query.getLimit() != null && query.getLimit() > 0 && entries.size() > query.getLimit()) {
            return entries.subList(entries.size() - query.getLimit(), entries.size());
        }
        return entries;
    }

    @Override
    public Money execute(GetProviderDebtQuery query) {
        if (query == null || query.getProviderId() == null) {
            return Money.zero();
        }
        return providerAccountRepository.findByProviderId(query.getProviderId())
                .map(ProviderAccount::getReceivableBalance)
                .orElse(Money.zero());
    }
}
