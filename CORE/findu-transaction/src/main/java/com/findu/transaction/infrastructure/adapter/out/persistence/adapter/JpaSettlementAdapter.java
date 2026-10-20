package com.findu.transaction.infrastructure.adapter.out.persistence.adapter;

import com.findu.transaction.application.port.out.persistence.SettlementRepository;
import com.findu.transaction.domain.enums.SettlementType;
import com.findu.transaction.domain.model.settlement.Settlement;
import com.findu.transaction.infrastructure.adapter.out.persistence.entity.SettlementDailyBalanceEntity;
import com.findu.transaction.infrastructure.adapter.out.persistence.entity.SettlementEntity;
import com.findu.transaction.infrastructure.adapter.out.persistence.mapper.TransactionPersistenceMapper;
import com.findu.transaction.infrastructure.adapter.out.persistence.repository.SpringDataSettlementDailyBalanceRepository;
import com.findu.transaction.infrastructure.adapter.out.persistence.repository.SpringDataSettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class JpaSettlementAdapter implements SettlementRepository {

    private final SpringDataSettlementRepository jpaRepository;
    private final SpringDataSettlementDailyBalanceRepository dailyBalanceLinkRepository;
    private final TransactionPersistenceMapper mapper;

    @Override
    public Settlement save(Settlement settlement) {
        SettlementEntity entity = mapper.toEntity(settlement);
        SettlementEntity saved = jpaRepository.save(entity);

        if (settlement.getDailyBalanceIds() != null && !settlement.getDailyBalanceIds().isEmpty()) {
            for (String dbId : settlement.getDailyBalanceIds()) {
                SettlementDailyBalanceEntity link = SettlementDailyBalanceEntity.builder()
                        .id(UUID.randomUUID().toString())
                        .settlementId(saved.getId())
                        .dailyBalanceId(dbId)
                        .createdAt(Instant.now())
                        .build();
                dailyBalanceLinkRepository.save(link);
            }
        }

        List<String> linkedDailyBalanceIds = dailyBalanceLinkRepository.findBySettlementId(saved.getId())
                .stream().map(SettlementDailyBalanceEntity::getDailyBalanceId).collect(Collectors.toList());

        return mapper.toDomain(saved, linkedDailyBalanceIds);
    }

    @Override
    public Optional<Settlement> findById(String id) {
        return jpaRepository.findById(id).map(entity -> {
            List<String> linked = dailyBalanceLinkRepository.findBySettlementId(entity.getId())
                    .stream().map(SettlementDailyBalanceEntity::getDailyBalanceId).collect(Collectors.toList());
            return mapper.toDomain(entity, linked);
        });
    }

    @Override
    public Optional<Settlement> findBySettlementCode(String settlementCode) {
        return jpaRepository.findBySettlementCode(settlementCode).map(entity -> {
            List<String> linked = dailyBalanceLinkRepository.findBySettlementId(entity.getId())
                    .stream().map(SettlementDailyBalanceEntity::getDailyBalanceId).collect(Collectors.toList());
            return mapper.toDomain(entity, linked);
        });
    }

    @Override
    public List<Settlement> findByProviderId(Long providerId) {
        return jpaRepository.findByProviderId(providerId).stream()
                .map(entity -> {
                    List<String> linked = dailyBalanceLinkRepository.findBySettlementId(entity.getId())
                            .stream().map(SettlementDailyBalanceEntity::getDailyBalanceId).collect(Collectors.toList());
                    return mapper.toDomain(entity, linked);
                })
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Settlement> findByProviderIdAndPeriodStartAndPeriodEndAndSettlementType(
            Long providerId, Instant periodStart, Instant periodEnd, SettlementType settlementType) {

        String typeStr = settlementType != null ? settlementType.name() : "DAILY";
        return jpaRepository.findByProviderIdAndPeriodStartAndPeriodEndAndSettlementType(providerId, periodStart, periodEnd, typeStr)
                .map(entity -> {
                    List<String> linked = dailyBalanceLinkRepository.findBySettlementId(entity.getId())
                            .stream().map(SettlementDailyBalanceEntity::getDailyBalanceId).collect(Collectors.toList());
                    return mapper.toDomain(entity, linked);
                });
    }

    @Override
    public List<String> findSettledDailyBalanceIdsByProviderId(Long providerId) {
        return dailyBalanceLinkRepository.findSettledDailyBalanceIdsByProviderId(providerId);
    }
}
