package com.findu.transaction.infrastructure.adapter.out.persistence.adapter;

import com.findu.transaction.application.port.out.persistence.ProviderAccountRepository;
import com.findu.transaction.domain.model.account.ProviderAccount;
import com.findu.transaction.domain.model.account.ProviderLedgerEntry;
import com.findu.transaction.infrastructure.adapter.out.persistence.entity.ProviderAccountEntity;
import com.findu.transaction.infrastructure.adapter.out.persistence.entity.ProviderLedgerEntryEntity;
import com.findu.transaction.infrastructure.adapter.out.persistence.mapper.TransactionPersistenceMapper;
import com.findu.transaction.infrastructure.adapter.out.persistence.repository.SpringDataProviderAccountRepository;
import com.findu.transaction.infrastructure.adapter.out.persistence.repository.SpringDataProviderLedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class JpaProviderAccountAdapter implements ProviderAccountRepository {

    private final SpringDataProviderAccountRepository accountJpaRepository;
    private final SpringDataProviderLedgerEntryRepository ledgerJpaRepository;
    private final TransactionPersistenceMapper mapper;

    @Override
    public ProviderAccount save(ProviderAccount providerAccount) {
        ProviderAccountEntity entity = mapper.toEntity(providerAccount);
        ProviderAccountEntity saved = accountJpaRepository.save(entity);

        if (providerAccount.getLedgerEntries() != null) {
            for (ProviderLedgerEntry entry : providerAccount.getLedgerEntries()) {
                if (!ledgerJpaRepository.existsById(entry.getEntryId())) {
                    ProviderLedgerEntryEntity entryEntity = mapper.toEntity(entry);
                    ledgerJpaRepository.save(entryEntity);
                }
            }
        }

        List<ProviderLedgerEntryEntity> ledgerEntities = ledgerJpaRepository.findByProviderIdOrderByCreatedAtAsc(saved.getProviderId());
        return mapper.toDomain(saved, ledgerEntities);
    }

    @Override
    public Optional<ProviderAccount> findByProviderId(Long providerId) {
        Optional<ProviderAccountEntity> accountOpt = accountJpaRepository.findByProviderId(providerId);
        if (accountOpt.isEmpty()) {
            return Optional.empty();
        }
        List<ProviderLedgerEntryEntity> ledgerEntities = ledgerJpaRepository.findByProviderIdOrderByCreatedAtAsc(providerId);
        return Optional.of(mapper.toDomain(accountOpt.get(), ledgerEntities));
    }

    @Override
    public List<ProviderAccount> findAll() {
        return accountJpaRepository.findAll().stream()
                .map(entity -> {
                    List<ProviderLedgerEntryEntity> ledgerEntities = ledgerJpaRepository.findByProviderIdOrderByCreatedAtAsc(entity.getProviderId());
                    return mapper.toDomain(entity, ledgerEntities);
                })
                .collect(Collectors.toList());
    }
}
