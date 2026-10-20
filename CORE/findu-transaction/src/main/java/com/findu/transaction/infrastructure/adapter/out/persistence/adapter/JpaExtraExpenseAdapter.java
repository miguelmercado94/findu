package com.findu.transaction.infrastructure.adapter.out.persistence.adapter;

import com.findu.transaction.application.port.out.persistence.ExtraExpenseRepository;
import com.findu.transaction.domain.model.expense.ExtraExpenseRequest;
import com.findu.transaction.infrastructure.adapter.out.persistence.entity.ExtraExpenseRequestEntity;
import com.findu.transaction.infrastructure.adapter.out.persistence.mapper.TransactionPersistenceMapper;
import com.findu.transaction.infrastructure.adapter.out.persistence.repository.SpringDataExtraExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class JpaExtraExpenseAdapter implements ExtraExpenseRepository {

    private final SpringDataExtraExpenseRepository jpaRepository;
    private final TransactionPersistenceMapper mapper;

    @Override
    public ExtraExpenseRequest save(ExtraExpenseRequest extraExpense) {
        ExtraExpenseRequestEntity entity = mapper.toEntity(extraExpense);
        ExtraExpenseRequestEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ExtraExpenseRequest> findById(String id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<ExtraExpenseRequest> findByServiceRequestId(Long serviceRequestId) {
        return jpaRepository.findByServiceRequestId(serviceRequestId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExtraExpenseRequest> findByProviderId(Long providerId) {
        return jpaRepository.findByProviderId(providerId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
