package com.findu.transaction.infrastructure.adapter.out.persistence.repository;

import com.findu.transaction.infrastructure.adapter.out.persistence.entity.ProviderDebtEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataProviderDebtRepository extends JpaRepository<ProviderDebtEntity, String> {
    List<ProviderDebtEntity> findByProviderId(Long providerId);
}
