package com.findu.security.infrastructure.adapter.cache;

import com.findu.security.domain.model.CachedTokenSession;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class NoOpTokenRevocationCacheAdapterTest {

    private final NoOpTokenRevocationCacheAdapter adapter = new NoOpTokenRevocationCacheAdapter();

    @Test
    void defaults() {
        StepVerifier.create(adapter.isAccessRevokedInCache("x"))
                .expectNext(false)
                .verifyComplete();
        StepVerifier.create(adapter.isRefreshRevokedInCache("x"))
                .expectNext(false)
                .verifyComplete();
        StepVerifier.create(adapter.putRevokedSession(new CachedTokenSession("a", "b", false), 1L))
                .verifyComplete();
        StepVerifier.create(adapter.removeExpiredRevokedEntries())
                .expectNext(0L)
                .verifyComplete();
    }
}
