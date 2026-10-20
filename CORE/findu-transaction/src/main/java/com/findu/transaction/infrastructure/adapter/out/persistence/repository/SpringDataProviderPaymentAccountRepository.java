package com.findu.transaction.infrastructure.adapter.out.persistence.repository;

import com.findu.transaction.infrastructure.adapter.out.persistence.entity.ProviderPaymentAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataProviderPaymentAccountRepository extends JpaRepository<ProviderPaymentAccountEntity, String> {
    List<ProviderPaymentAccountEntity> findByProviderId(Long providerId);
}
