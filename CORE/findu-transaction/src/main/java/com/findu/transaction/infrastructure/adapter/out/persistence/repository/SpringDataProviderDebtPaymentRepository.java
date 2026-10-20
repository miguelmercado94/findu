package com.findu.transaction.infrastructure.adapter.out.persistence.repository;

import com.findu.transaction.infrastructure.adapter.out.persistence.entity.ProviderDebtPaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpringDataProviderDebtPaymentRepository extends JpaRepository<ProviderDebtPaymentEntity, String> {
    Optional<ProviderDebtPaymentEntity> findByPaymentCode(String paymentCode);
    List<ProviderDebtPaymentEntity> findByProviderId(Long providerId);
}
