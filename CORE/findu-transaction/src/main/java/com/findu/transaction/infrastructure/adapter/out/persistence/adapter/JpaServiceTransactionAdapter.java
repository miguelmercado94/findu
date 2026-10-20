package com.findu.transaction.infrastructure.adapter.out.persistence.adapter;

import com.findu.transaction.application.port.out.persistence.ServiceTransactionRepository;
import com.findu.transaction.domain.model.transaction.ServiceTransaction;
import com.findu.transaction.domain.valueobject.TransactionCode;
import com.findu.transaction.infrastructure.adapter.out.persistence.entity.ServiceTransactionEntity;
import com.findu.transaction.infrastructure.adapter.out.persistence.mapper.TransactionPersistenceMapper;
import com.findu.transaction.infrastructure.adapter.out.persistence.repository.SpringDataServiceTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class JpaServiceTransactionAdapter implements ServiceTransactionRepository {

    private final SpringDataServiceTransactionRepository jpaRepository;
    private final TransactionPersistenceMapper mapper;

    @Override
    public ServiceTransaction save(ServiceTransaction transaction) {
        ServiceTransactionEntity entity = mapper.toEntity(transaction);
        ServiceTransactionEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ServiceTransaction> findById(String id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<ServiceTransaction> findByTransactionCode(TransactionCode code) {
        if (code == null || code.getValue() == null) return Optional.empty();
        return jpaRepository.findByTransactionCode(code.getValue()).map(mapper::toDomain);
    }

    @Override
    public Optional<ServiceTransaction> findByServiceRequestId(Long serviceRequestId) {
        return jpaRepository.findByServiceRequestId(serviceRequestId).map(mapper::toDomain);
    }

    @Override
    public List<ServiceTransaction> findByProviderId(Long providerId) {
        return jpaRepository.findByProviderId(providerId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ServiceTransaction> findByCustomerId(Long customerId) {
        return jpaRepository.findByCustomerId(customerId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ServiceTransaction> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
