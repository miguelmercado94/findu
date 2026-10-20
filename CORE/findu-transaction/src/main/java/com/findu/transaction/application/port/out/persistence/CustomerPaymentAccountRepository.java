package com.findu.transaction.application.port.out.persistence;

import com.findu.transaction.infrastructure.adapter.out.persistence.entity.CustomerPaymentAccountEntity;

import java.util.List;
import java.util.Optional;

public interface CustomerPaymentAccountRepository {
    CustomerPaymentAccountEntity save(CustomerPaymentAccountEntity account);
    Optional<CustomerPaymentAccountEntity> findById(String id);
    List<CustomerPaymentAccountEntity> findByCustomerId(Long customerId);
}
