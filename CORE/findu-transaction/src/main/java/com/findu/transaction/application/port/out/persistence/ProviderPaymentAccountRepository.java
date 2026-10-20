package com.findu.transaction.application.port.out.persistence;

import com.findu.transaction.infrastructure.adapter.out.persistence.entity.ProviderPaymentAccountEntity;

import java.util.List;
import java.util.Optional;

public interface ProviderPaymentAccountRepository {
    ProviderPaymentAccountEntity save(ProviderPaymentAccountEntity account);
    Optional<ProviderPaymentAccountEntity> findById(String id);
    List<ProviderPaymentAccountEntity> findByProviderId(Long providerId);
}
