package com.findu.transaction.dailybalance.repository;

import com.findu.transaction.dailybalance.entity.DailyBalanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailyBalanceJpaRepository extends JpaRepository<DailyBalanceEntity, String> {

    Optional<DailyBalanceEntity> findByProviderIdAndBalanceDate(Long providerId, LocalDate balanceDate);

    boolean existsByProviderIdAndBalanceDate(Long providerId, LocalDate balanceDate);
}
