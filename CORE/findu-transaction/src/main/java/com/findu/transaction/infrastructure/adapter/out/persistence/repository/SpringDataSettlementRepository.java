package com.findu.transaction.infrastructure.adapter.out.persistence.repository;

import com.findu.transaction.infrastructure.adapter.out.persistence.entity.SettlementEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SpringDataSettlementRepository extends JpaRepository<SettlementEntity, String> {
    Optional<SettlementEntity> findBySettlementCode(String settlementCode);
    List<SettlementEntity> findByProviderId(Long providerId);
    Optional<SettlementEntity> findByProviderIdAndPeriodStartAndPeriodEndAndSettlementType(
            Long providerId, Instant periodStart, Instant periodEnd, String settlementType);
}
