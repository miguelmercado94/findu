package com.findu.transaction.infrastructure.adapter.out.persistence.adapter;

import com.findu.transaction.application.port.out.persistence.ProviderPaymentAccountRepository;
import com.findu.transaction.infrastructure.adapter.out.persistence.entity.ProviderPaymentAccountEntity;
import com.findu.transaction.infrastructure.adapter.out.persistence.repository.SpringDataProviderPaymentAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaProviderPaymentAccountAdapter implements ProviderPaymentAccountRepository {

    private final SpringDataProviderPaymentAccountRepository jpaRepository;

    @Override
    public ProviderPaymentAccountEntity save(ProviderPaymentAccountEntity account) {
        return jpaRepository.save(account);
    }

    @Override
    public Optional<ProviderPaymentAccountEntity> findById(String id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<ProviderPaymentAccountEntity> findByProviderId(Long providerId) {
        return jpaRepository.findByProviderId(providerId);
    }
}
