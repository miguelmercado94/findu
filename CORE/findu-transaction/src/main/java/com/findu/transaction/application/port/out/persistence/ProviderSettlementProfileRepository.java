package com.findu.transaction.application.port.out.persistence;

import com.findu.transaction.domain.model.settlement.ProviderSettlementProfile;

import java.util.Optional;

public interface ProviderSettlementProfileRepository {
    ProviderSettlementProfile save(ProviderSettlementProfile profile);
    Optional<ProviderSettlementProfile> findByProviderId(Long providerId);
}
