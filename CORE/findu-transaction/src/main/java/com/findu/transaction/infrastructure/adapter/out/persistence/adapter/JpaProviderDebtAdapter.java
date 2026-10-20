package com.findu.transaction.infrastructure.adapter.out.persistence.adapter;

import com.findu.transaction.application.port.out.persistence.ProviderDebtRepository;
import com.findu.transaction.infrastructure.adapter.out.persistence.entity.ProviderDebtEntity;
import com.findu.transaction.infrastructure.adapter.out.persistence.repository.SpringDataProviderDebtRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaProviderDebtAdapter implements ProviderDebtRepository {

    private final SpringDataProviderDebtRepository jpaRepository;

    @Override
    public ProviderDebtEntity save(ProviderDebtEntity debt) {
        return jpaRepository.save(debt);
    }

    @Override
    public Optional<ProviderDebtEntity> findById(String id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<ProviderDebtEntity> findByProviderId(Long providerId) {
        return jpaRepository.findByProviderId(providerId);
    }
}
