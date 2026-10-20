package com.findu.transaction.infrastructure.adapter.out.persistence.repository;

import com.findu.transaction.infrastructure.adapter.out.persistence.entity.CustomerPaymentAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataCustomerPaymentAccountRepository extends JpaRepository<CustomerPaymentAccountEntity, String> {
    List<CustomerPaymentAccountEntity> findByCustomerId(Long customerId);
}
