package com.findu.transaction.application.port.out.persistence;

import com.findu.transaction.domain.model.transaction.ServiceTransaction;
import com.findu.transaction.domain.valueobject.TransactionCode;

import java.util.List;
import java.util.Optional;

public interface ServiceTransactionRepository {
    ServiceTransaction save(ServiceTransaction transaction);
    Optional<ServiceTransaction> findById(String id);
    Optional<ServiceTransaction> findByTransactionCode(TransactionCode code);
    Optional<ServiceTransaction> findByServiceRequestId(Long serviceRequestId);
    List<ServiceTransaction> findByProviderId(Long providerId);
    List<ServiceTransaction> findByCustomerId(Long customerId);
    List<ServiceTransaction> findAll();
}
