package com.findu.transaction.infrastructure.adapter.out.persistence.adapter;

import com.findu.transaction.application.port.out.persistence.CustomerPaymentAccountRepository;
import com.findu.transaction.infrastructure.adapter.out.persistence.entity.CustomerPaymentAccountEntity;
import com.findu.transaction.infrastructure.adapter.out.persistence.repository.SpringDataCustomerPaymentAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaCustomerPaymentAccountAdapter implements CustomerPaymentAccountRepository {

    private final SpringDataCustomerPaymentAccountRepository jpaRepository;

    @Override
    public CustomerPaymentAccountEntity save(CustomerPaymentAccountEntity account) {
        return jpaRepository.save(account);
    }

    @Override
    public Optional<CustomerPaymentAccountEntity> findById(String id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<CustomerPaymentAccountEntity> findByCustomerId(Long customerId) {
        return jpaRepository.findByCustomerId(customerId);
    }
}
