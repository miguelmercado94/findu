package com.findu.transaction.infrastructure.adapter.out.persistence.repository;

import com.findu.transaction.infrastructure.adapter.out.persistence.entity.ProviderAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataProviderAccountRepository extends JpaRepository<ProviderAccountEntity, String> {
    Optional<ProviderAccountEntity> findByProviderId(Long providerId);
}
