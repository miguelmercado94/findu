package com.findu.transaction.infrastructure.adapter.out.persistence.repository;

import com.findu.transaction.infrastructure.adapter.out.persistence.entity.ProviderLedgerEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataProviderLedgerEntryRepository extends JpaRepository<ProviderLedgerEntryEntity, String> {
    List<ProviderLedgerEntryEntity> findByProviderIdOrderByCreatedAtAsc(Long providerId);
}
