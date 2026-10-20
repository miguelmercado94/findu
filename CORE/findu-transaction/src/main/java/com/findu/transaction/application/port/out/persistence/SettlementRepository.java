package com.findu.transaction.application.port.out.persistence;

import com.findu.transaction.domain.enums.SettlementType;
import com.findu.transaction.domain.model.settlement.Settlement;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SettlementRepository {
    Settlement save(Settlement settlement);
    Optional<Settlement> findById(String id);
    Optional<Settlement> findBySettlementCode(String settlementCode);
    List<Settlement> findByProviderId(Long providerId);
    Optional<Settlement> findByProviderIdAndPeriodStartAndPeriodEndAndSettlementType(
            Long providerId, Instant periodStart, Instant periodEnd, SettlementType settlementType);
    List<String> findSettledDailyBalanceIdsByProviderId(Long providerId);
}
