package com.findu.transaction.infrastructure.adapter.out.persistence.adapter;

import com.findu.transaction.application.port.out.persistence.RefundRepository;
import com.findu.transaction.domain.enums.RefundStatus;
import com.findu.transaction.domain.model.refund.Refund;
import com.findu.transaction.infrastructure.adapter.out.persistence.entity.RefundEntity;
import com.findu.transaction.infrastructure.adapter.out.persistence.mapper.TransactionPersistenceMapper;
import com.findu.transaction.infrastructure.adapter.out.persistence.repository.SpringDataRefundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class JpaRefundAdapter implements RefundRepository {

    private final SpringDataRefundRepository jpaRepository;
    private final TransactionPersistenceMapper mapper;

    @Override
    public Refund save(Refund refund) {
        RefundEntity entity = mapper.toEntity(refund);
        RefundEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Refund> findById(String id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Refund> findByRefundCode(String refundCode) {
        return jpaRepository.findByRefundCode(refundCode).map(mapper::toDomain);
    }

    @Override
    public List<Refund> findByStatus(RefundStatus status) {
        String statusStr = status != null ? status.name() : null;
        return jpaRepository.findByStatus(statusStr).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Refund> findByCustomerId(Long customerId) {
        return jpaRepository.findByCustomerId(customerId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
