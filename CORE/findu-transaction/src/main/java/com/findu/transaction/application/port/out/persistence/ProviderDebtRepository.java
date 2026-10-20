package com.findu.transaction.application.port.out.persistence;

import com.findu.transaction.infrastructure.adapter.out.persistence.entity.ProviderDebtEntity;

import java.util.List;
import java.util.Optional;

public interface ProviderDebtRepository {
    ProviderDebtEntity save(ProviderDebtEntity debt);
    Optional<ProviderDebtEntity> findById(String id);
    List<ProviderDebtEntity> findByProviderId(Long providerId);
}
