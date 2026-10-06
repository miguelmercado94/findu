package com.findu.transaction.dailybalance.repository;

import com.findu.transaction.dailybalance.entity.ServiceTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ServiceTransactionQueryRepository extends JpaRepository<ServiceTransactionEntity, String> {

    @Query("SELECT DISTINCT t.providerId FROM ServiceTransactionEntity t WHERE t.createdAt >= :start AND t.createdAt < :end")
    List<Long> findActiveProviderIdsBetween(@Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT t FROM ServiceTransactionEntity t WHERE t.providerId = :providerId AND t.createdAt >= :start AND t.createdAt < :end")
    List<ServiceTransactionEntity> findByProviderIdAndCreatedAtBetween(
            @Param("providerId") Long providerId,
            @Param("start") Instant start,
            @Param("end") Instant end);
}
