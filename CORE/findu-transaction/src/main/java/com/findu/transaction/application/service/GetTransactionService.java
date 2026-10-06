package com.findu.transaction.application.service;

import com.findu.transaction.application.port.in.GetCustomerTransactionsUseCase;
import com.findu.transaction.application.port.in.GetFinancialQueryUseCase;
import com.findu.transaction.application.port.in.GetTransactionUseCase;
import com.findu.transaction.application.port.in.query.GetCustomerTransactionsQuery;
import com.findu.transaction.application.port.in.query.GetTransactionQuery;
import com.findu.transaction.application.port.out.persistence.ProviderAccountRepository;
import com.findu.transaction.application.port.out.persistence.ServiceTransactionRepository;
import com.findu.transaction.domain.model.account.ProviderAccount;
import com.findu.transaction.domain.model.transaction.ServiceTransaction;
import com.findu.transaction.domain.valueobject.TransactionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GetTransactionService implements GetTransactionUseCase, GetCustomerTransactionsUseCase, GetFinancialQueryUseCase {

    private final ServiceTransactionRepository serviceTransactionRepository;
    private final ProviderAccountRepository providerAccountRepository;

    @Override
    public Optional<ServiceTransaction> execute(GetTransactionQuery query) {
        if (query == null) {
            return Optional.empty();
        }
        if (query.getTransactionId() != null) {
            return serviceTransactionRepository.findById(query.getTransactionId());
        }
        if (query.getTransactionCode() != null) {
            return serviceTransactionRepository.findByTransactionCode(new TransactionCode(query.getTransactionCode()));
        }
        return Optional.empty();
    }

    @Override
    public List<ServiceTransaction> execute(GetCustomerTransactionsQuery query) {
        if (query == null || query.getCustomerId() == null) {
            return List.of();
        }
        return serviceTransactionRepository.findByCustomerId(query.getCustomerId());
    }

    // Methods for backwards compatibility with GetFinancialQueryUseCase
    @Override
    public Optional<ServiceTransaction> getTransactionById(String id) {
        return serviceTransactionRepository.findById(id);
    }

    @Override
    public Optional<ServiceTransaction> getTransactionByCode(String transactionCode) {
        return serviceTransactionRepository.findByTransactionCode(new TransactionCode(transactionCode));
    }

    @Override
    public List<ServiceTransaction> getTransactionsByProviderId(Long providerId) {
        return serviceTransactionRepository.findByProviderId(providerId);
    }

    @Override
    public Optional<ProviderAccount> getProviderAccount(Long providerId) {
        return providerAccountRepository.findByProviderId(providerId);
    }
}
