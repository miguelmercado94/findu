package com.findu.transaction.application.port.out.persistence;

import com.findu.transaction.domain.model.finance.BankConsignmentReport;

import java.util.List;
import java.util.Optional;

public interface BankConsignmentReportRepository {
    BankConsignmentReport save(BankConsignmentReport report);
    Optional<BankConsignmentReport> findById(String id);
    Optional<BankConsignmentReport> findByReference(String reference);
    List<BankConsignmentReport> findByProviderId(Long providerId);
}
