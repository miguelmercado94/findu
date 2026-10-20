package com.findu.transaction.infrastructure.adapter.out.persistence.adapter;

import com.findu.transaction.application.port.out.persistence.DailyBalanceRepository;
import com.findu.transaction.domain.model.balance.DailyBalance;
import com.findu.transaction.infrastructure.adapter.out.persistence.entity.DailyBalanceEntity;
import com.findu.transaction.infrastructure.adapter.out.persistence.mapper.TransactionPersistenceMapper;
import com.findu.transaction.infrastructure.adapter.out.persistence.repository.SpringDataDailyBalanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class JpaDailyBalanceAdapter implements DailyBalanceRepository {

    private final SpringDataDailyBalanceRepository jpaRepository;
    private final TransactionPersistenceMapper mapper;

    @Override
    public DailyBalance save(DailyBalance dailyBalance) {
        DailyBalanceEntity entity = mapper.toEntity(dailyBalance);
        DailyBalanceEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<DailyBalance> findByProviderIdAndBalanceDate(Long providerId, LocalDate balanceDate) {
        return jpaRepository.findByProviderIdAndBalanceDate(providerId, balanceDate).map(mapper::toDomain);
    }

    @Override
    public List<DailyBalance> findByProviderId(Long providerId) {
        return jpaRepository.findByProviderId(providerId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<DailyBalance> findByProviderIdAndBalanceDateBetween(Long providerId, LocalDate startDate, LocalDate endDate) {
        return jpaRepository.findByProviderIdAndBalanceDateBetween(providerId, startDate, endDate).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
