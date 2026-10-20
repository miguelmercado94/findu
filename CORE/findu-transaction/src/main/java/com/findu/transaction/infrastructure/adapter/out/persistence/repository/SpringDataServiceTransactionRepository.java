package com.findu.transaction.infrastructure.adapter.out.persistence.repository;

import com.findu.transaction.infrastructure.adapter.out.persistence.entity.ServiceTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpringDataServiceTransactionRepository extends JpaRepository<ServiceTransactionEntity, String> {
    Optional<ServiceTransactionEntity> findByTransactionCode(String transactionCode);
    Optional<ServiceTransactionEntity> findByServiceRequestId(Long serviceRequestId);
    List<ServiceTransactionEntity> findByProviderId(Long providerId);
    List<ServiceTransactionEntity> findByCustomerId(Long customerId);
}
