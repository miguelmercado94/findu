package com.findu.transaction.dailybalance.repository;

import com.findu.transaction.dailybalance.entity.ExtraExpenseRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ExtraExpenseQueryRepository extends JpaRepository<ExtraExpenseRequestEntity, String> {

    @Query("SELECT e FROM ExtraExpenseRequestEntity e WHERE e.providerId = :providerId AND e.status = 'APPROVED' AND e.approvedAt >= :start AND e.approvedAt < :end")
    List<ExtraExpenseRequestEntity> findApprovedByProviderIdAndApprovedAtBetween(
            @Param("providerId") Long providerId,
            @Param("start") Instant start,
            @Param("end") Instant end);
}
