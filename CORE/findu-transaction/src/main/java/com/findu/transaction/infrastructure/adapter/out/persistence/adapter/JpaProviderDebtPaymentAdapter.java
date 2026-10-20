package com.findu.transaction.infrastructure.adapter.out.persistence.adapter;

import com.findu.transaction.application.port.out.persistence.ProviderDebtPaymentRepository;
import com.findu.transaction.domain.model.debt.ProviderDebtPayment;
import com.findu.transaction.infrastructure.adapter.out.persistence.entity.ProviderDebtPaymentEntity;
import com.findu.transaction.infrastructure.adapter.out.persistence.mapper.TransactionPersistenceMapper;
import com.findu.transaction.infrastructure.adapter.out.persistence.repository.SpringDataProviderDebtPaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class JpaProviderDebtPaymentAdapter implements ProviderDebtPaymentRepository {

    private final SpringDataProviderDebtPaymentRepository jpaRepository;
    private final TransactionPersistenceMapper mapper;

    @Override
    public ProviderDebtPayment save(ProviderDebtPayment payment) {
        ProviderDebtPaymentEntity entity = mapper.toEntity(payment);
        ProviderDebtPaymentEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ProviderDebtPayment> findById(String id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<ProviderDebtPayment> findByProviderId(Long providerId) {
        return jpaRepository.findByProviderId(providerId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
