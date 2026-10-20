package com.findu.transaction.infrastructure.adapter.out.persistence.repository;

import com.findu.transaction.infrastructure.adapter.out.persistence.entity.ProviderSettlementProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataProviderSettlementProfileRepository extends JpaRepository<ProviderSettlementProfileEntity, String> {
    Optional<ProviderSettlementProfileEntity> findByProviderId(Long providerId);
}
