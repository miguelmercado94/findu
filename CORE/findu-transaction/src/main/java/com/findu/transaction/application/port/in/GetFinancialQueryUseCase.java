package com.findu.transaction.application.port.in;

import com.findu.transaction.domain.model.account.ProviderAccount;
import com.findu.transaction.domain.model.transaction.ServiceTransaction;

import java.util.List;
import java.util.Optional;

public interface GetFinancialQueryUseCase {
    Optional<ServiceTransaction> getTransactionById(String id);
    Optional<ServiceTransaction> getTransactionByCode(String transactionCode);
    List<ServiceTransaction> getTransactionsByProviderId(Long providerId);
    Optional<ProviderAccount> getProviderAccount(Long providerId);
}
