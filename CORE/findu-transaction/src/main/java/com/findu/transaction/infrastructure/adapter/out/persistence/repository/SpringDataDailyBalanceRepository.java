package com.findu.transaction.infrastructure.adapter.out.persistence.repository;

import com.findu.transaction.infrastructure.adapter.out.persistence.entity.DailyBalanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SpringDataDailyBalanceRepository extends JpaRepository<DailyBalanceEntity, String> {
    Optional<DailyBalanceEntity> findByProviderIdAndBalanceDate(Long providerId, LocalDate balanceDate);
    List<DailyBalanceEntity> findByProviderId(Long providerId);
    List<DailyBalanceEntity> findByProviderIdAndBalanceDateBetween(Long providerId, LocalDate startDate, LocalDate endDate);
}
