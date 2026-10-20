package com.findu.transaction.infrastructure.adapter.out.persistence.adapter;

import com.findu.transaction.application.port.out.persistence.PayoutRepository;
import com.findu.transaction.domain.enums.PayoutStatus;
import com.findu.transaction.domain.model.payout.Payout;
import com.findu.transaction.infrastructure.adapter.out.persistence.entity.PayoutEntity;
import com.findu.transaction.infrastructure.adapter.out.persistence.mapper.TransactionPersistenceMapper;
import com.findu.transaction.infrastructure.adapter.out.persistence.repository.SpringDataPayoutRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class JpaPayoutAdapter implements PayoutRepository {

    private final SpringDataPayoutRepository jpaRepository;
    private final TransactionPersistenceMapper mapper;

    @Override
    public Payout save(Payout payout) {
        PayoutEntity entity = mapper.toEntity(payout);
        PayoutEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Payout> findById(String id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Payout> findByPayoutCode(String payoutCode) {
        return jpaRepository.findByPayoutCode(payoutCode).map(mapper::toDomain);
    }

    @Override
    public List<Payout> findByStatus(PayoutStatus status) {
        String statusStr = status != null ? status.name() : null;
        return jpaRepository.findByStatus(statusStr).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Payout> findByProviderId(Long providerId) {
        return jpaRepository.findByProviderId(providerId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
