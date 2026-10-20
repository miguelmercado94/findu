package com.findu.transaction.infrastructure.adapter.out.persistence.repository;

import com.findu.transaction.infrastructure.adapter.out.persistence.entity.PayoutEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpringDataPayoutRepository extends JpaRepository<PayoutEntity, String> {
    Optional<PayoutEntity> findByPayoutCode(String payoutCode);
    List<PayoutEntity> findByStatus(String status);
    List<PayoutEntity> findByProviderId(Long providerId);
}
