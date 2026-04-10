package com.findu.security.infrastructure.adapter.cache;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RedisTokenCacheKeysTest {

    @Test
    void accessKey_and_refreshKey_usePrefixAndHash() {
        String a = RedisTokenCacheKeys.accessKey("pfx:", "jwt-a");
        String r = RedisTokenCacheKeys.refreshKey("pfx:", "jwt-r");
        assertThat(a).startsWith("pfx:access:");
        assertThat(r).startsWith("pfx:refresh:");
        assertThat(a).isNotEqualTo(RedisTokenCacheKeys.accessKey("pfx:", "other"));
    }

    @Test
    void accessKeyPattern() {
        assertThat(RedisTokenCacheKeys.accessKeyPattern("app:")).isEqualTo("app:access:*");
    }
}
