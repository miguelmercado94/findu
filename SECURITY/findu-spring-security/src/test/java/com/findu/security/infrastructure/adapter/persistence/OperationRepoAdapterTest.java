package com.findu.security.infrastructure.adapter.persistence;

import com.findu.security.domain.model.Operation;
import com.findu.security.infrastructure.entity.OperationEntity;
import com.findu.security.infrastructure.repository.OperationRepository;
import com.findu.security.mapper.OperationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationRepoAdapterTest {

    @Mock
    private OperationRepository operationRepository;

    @Mock
    private OperationMapper operationMapper;

    private OperationRepoAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new OperationRepoAdapter(operationRepository, operationMapper);
    }

    @Test
    void findByModulePathBaseAndPathAndHttpMethod_mapsEntity() {
        OperationEntity entity = new OperationEntity();
        entity.setId(1L);
        entity.setPath("/api/v1/profile");
        entity.setName("PROFILE_READ");
        entity.setHttpMethod("GET");
        entity.setModuleId(3L);
        entity.setPermiteAll(false);
        entity.setActive(true);

        Operation domain = new Operation();
        domain.setName("PROFILE_READ");
        domain.setPermiteAll(false);
        domain.setActive(true);

        when(operationRepository.findByModulePathBaseAndPathAndHttpMethod(eq("security-auth"), eq("/api/v1/profile"), eq("GET")))
                .thenReturn(Mono.just(entity));
        when(operationMapper.toDomain(entity)).thenReturn(domain);

        Mono<Operation> result = adapter.findByModulePathBaseAndPathAndHttpMethod("security-auth", "/api/v1/profile", "GET");

        StepVerifier.create(result)
                .expectNextMatches(op -> "PROFILE_READ".equals(op.getName()) && !op.isPermiteAll())
                .verifyComplete();
    }
}
