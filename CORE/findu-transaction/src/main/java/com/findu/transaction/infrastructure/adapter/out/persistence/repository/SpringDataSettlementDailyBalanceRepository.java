package com.findu.transaction.infrastructure.adapter.out.persistence.repository;

import com.findu.transaction.infrastructure.adapter.out.persistence.entity.SettlementDailyBalanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SpringDataSettlementDailyBalanceRepository extends JpaRepository<SettlementDailyBalanceEntity, String> {

    List<SettlementDailyBalanceEntity> findBySettlementId(String settlementId);

    @Query("SELECT sdb.dailyBalanceId FROM SettlementDailyBalanceEntity sdb WHERE sdb.settlementId IN " +
           "(SELECT s.id FROM SettlementEntity s WHERE s.providerId = :providerId)")
    List<String> findSettledDailyBalanceIdsByProviderId(@Param("providerId") Long providerId);
}
