package com.findu.transaction.infrastructure.adapter.out.persistence.adapter;

import com.findu.transaction.application.port.out.persistence.ProviderSettlementProfileRepository;
import com.findu.transaction.domain.model.settlement.ProviderSettlementProfile;
import com.findu.transaction.infrastructure.adapter.out.persistence.entity.ProviderSettlementProfileEntity;
import com.findu.transaction.infrastructure.adapter.out.persistence.mapper.TransactionPersistenceMapper;
import com.findu.transaction.infrastructure.adapter.out.persistence.repository.SpringDataProviderSettlementProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaProviderSettlementProfileAdapter implements ProviderSettlementProfileRepository {

    private final SpringDataProviderSettlementProfileRepository jpaRepository;
    private final TransactionPersistenceMapper mapper;

    @Override
    public ProviderSettlementProfile save(ProviderSettlementProfile profile) {
        ProviderSettlementProfileEntity entity = mapper.toEntity(profile);
        ProviderSettlementProfileEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ProviderSettlementProfile> findByProviderId(Long providerId) {
        return jpaRepository.findByProviderId(providerId).map(mapper::toDomain);
    }
}
