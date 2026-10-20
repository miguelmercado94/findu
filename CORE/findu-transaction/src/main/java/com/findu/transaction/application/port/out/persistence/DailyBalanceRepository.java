package com.findu.transaction.application.port.out.persistence;

import com.findu.transaction.domain.model.balance.DailyBalance;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyBalanceRepository {
    DailyBalance save(DailyBalance balance);
    Optional<DailyBalance> findByProviderIdAndBalanceDate(Long providerId, LocalDate balanceDate);
    List<DailyBalance> findByProviderId(Long providerId);
    List<DailyBalance> findByProviderIdAndBalanceDateBetween(Long providerId, LocalDate startDate, LocalDate endDate);
}
