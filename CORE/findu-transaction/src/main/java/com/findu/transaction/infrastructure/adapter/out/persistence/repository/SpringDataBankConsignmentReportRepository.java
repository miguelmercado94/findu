package com.findu.transaction.infrastructure.adapter.out.persistence.repository;

import com.findu.transaction.infrastructure.adapter.out.persistence.entity.BankConsignmentReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpringDataBankConsignmentReportRepository extends JpaRepository<BankConsignmentReportEntity, String> {
    Optional<BankConsignmentReportEntity> findByReference(String reference);
    List<BankConsignmentReportEntity> findByProviderIdOrderByCreatedAtDesc(Long providerId);
}
