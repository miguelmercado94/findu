package com.findu.transaction.application.port.out.persistence;

import com.findu.transaction.domain.model.account.ProviderAccount;

import java.util.List;
import java.util.Optional;

public interface ProviderAccountRepository {
    ProviderAccount save(ProviderAccount account);
    Optional<ProviderAccount> findByProviderId(Long providerId);
    List<ProviderAccount> findAll();
}
