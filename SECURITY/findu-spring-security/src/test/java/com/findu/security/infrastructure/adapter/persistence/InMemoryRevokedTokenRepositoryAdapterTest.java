package com.findu.security.infrastructure.adapter.persistence;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class InMemoryRevokedTokenRepositoryAdapterTest {

    private InMemoryRevokedTokenRepositoryAdapter repo;

    @BeforeEach
    void setUp() {
        repo = new InMemoryRevokedTokenRepositoryAdapter();
    }

    @Test
    void save_and_block_flow() {
        String access = "header.payload.sig-access";
        String refresh = "header.payload.sig-refresh";

        StepVerifier.create(repo.saveTokenPair(access, refresh, Instant.now().getEpochSecond() + 1000))
                .verifyComplete();

        StepVerifier.create(repo.isAccessBlocked(access))
                .expectNext(false)
                .verifyComplete();

        StepVerifier.create(repo.markSessionUnavailable(access, refresh, Instant.now().getEpochSecond() + 100))
                .verifyComplete();

        StepVerifier.create(repo.isAccessBlocked(access))
                .expectNext(true)
                .verifyComplete();
    }
}
