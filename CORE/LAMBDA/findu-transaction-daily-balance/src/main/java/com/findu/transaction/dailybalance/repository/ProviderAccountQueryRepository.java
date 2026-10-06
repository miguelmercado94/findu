package com.findu.transaction.dailybalance.repository;

import com.findu.transaction.dailybalance.entity.ProviderAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProviderAccountQueryRepository extends JpaRepository<ProviderAccountEntity, String> {

    Optional<ProviderAccountEntity> findByProviderId(Long providerId);

    List<ProviderAccountEntity> findAll();
}
