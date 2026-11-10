package com.findu.dispatcher.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CoreProfileClientTest {

    private final AtomicInteger calls = new AtomicInteger();
    private final AtomicReference<String> lastKey = new AtomicReference<>();
    private final AtomicReference<String> lastPath = new AtomicReference<>();
    private final AtomicLong now = new AtomicLong();

    private CoreProfileClient client(ExchangeFunction fn) {
        ExchangeFunction counting = req -> {
            calls.incrementAndGet();
            lastKey.set(req.headers().getFirst("X-Internal-Service-Key"));
            lastPath.set(req.url().getPath());
            return fn.exchange(req);
        };
        return new CoreProfileClient(WebClient.builder().baseUrl("http://core/findu-core").exchangeFunction(counting).build(),
                "k", Duration.ofMillis(200), Duration.ofSeconds(300), now::get);
    }

    private static Mono<ClientResponse> json(HttpStatus s, String body) {
        return Mono.just(ClientResponse.create(s).header("Content-Type", MediaType.APPLICATION_JSON_VALUE).body(body).build());
    }

    @Test
    void resuelveEnviaLlaveYCachea() {
        CoreProfileClient c = client(r -> json(HttpStatus.OK, "{\"perfilId\":42,\"authUserId\":10}"));
        StepVerifier.create(c.resolveProveedorId("10")).expectNext(Optional.of(42L)).verifyComplete();
        StepVerifier.create(c.resolveProveedorId("10")).expectNext(Optional.of(42L)).verifyComplete();
        assertThat(calls.get()).isEqualTo(1);
        assertThat(lastKey.get()).isEqualTo("k");
        assertThat(lastPath.get()).isEqualTo("/findu-core/internal/perfiles/by-auth/10");
    }

    @Test
    void cacheExpiraTrasElTtl() {
        CoreProfileClient c = client(r -> json(HttpStatus.OK, "{\"perfilId\":42,\"authUserId\":10}"));
        c.resolveProveedorId("10").block();
        now.addAndGet(Duration.ofSeconds(301).toNanos());
        c.resolveProveedorId("10").block();
        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    void notFoundEsVacioYNoSeCachea() {
        CoreProfileClient c = client(r -> json(HttpStatus.NOT_FOUND, "{}"));
        StepVerifier.create(c.resolveProveedorId("10")).expectNext(Optional.empty()).verifyComplete();
        c.resolveProveedorId("10").block();
        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    void error5xxEsCoreUnavailable() {
        CoreProfileClient c = client(r -> json(HttpStatus.INTERNAL_SERVER_ERROR, "{}"));
        StepVerifier.create(c.resolveProveedorId("10"))
                .expectError(CoreProfileClient.CoreUnavailableException.class).verify();
    }

    @Test
    void fallaDeRedEsCoreUnavailable() {
        CoreProfileClient c = client(r -> Mono.error(new java.net.ConnectException("refused")));
        StepVerifier.create(c.resolveProveedorId("10"))
                .expectError(CoreProfileClient.CoreUnavailableException.class).verify();
    }

    @Test
    void timeoutEsCoreUnavailable() {
        CoreProfileClient c = client(r -> Mono.never());
        StepVerifier.create(c.resolveProveedorId("10"))
                .expectError(CoreProfileClient.CoreUnavailableException.class).verify(Duration.ofSeconds(3));
    }
}
