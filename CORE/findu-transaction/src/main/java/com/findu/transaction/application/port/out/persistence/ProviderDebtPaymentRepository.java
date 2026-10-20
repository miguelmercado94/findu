package com.findu.transaction.application.port.out.persistence;

import com.findu.transaction.domain.model.debt.ProviderDebtPayment;

import java.util.List;
import java.util.Optional;

public interface ProviderDebtPaymentRepository {
    ProviderDebtPayment save(ProviderDebtPayment payment);
    Optional<ProviderDebtPayment> findById(String id);
    List<ProviderDebtPayment> findByProviderId(Long providerId);
}
