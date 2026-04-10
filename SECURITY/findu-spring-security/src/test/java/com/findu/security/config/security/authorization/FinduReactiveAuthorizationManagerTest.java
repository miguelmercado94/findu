package com.findu.security.config.security.authorization;

import com.findu.security.application.port.output.persistence.OperationRepositoryPort;
import com.findu.security.domain.model.Operation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinduReactiveAuthorizationManagerTest {

    @Mock
    private OperationRepositoryPort operationRepositoryPort;

    private FinduReactiveAuthorizationManager manager;

    @BeforeEach
    void setUp() {
        manager = new FinduReactiveAuthorizationManager(operationRepositoryPort, "/security-auth");
    }

    @Test
    void authorize_authenticated_hasAuthority_allows() {
        Operation op = new Operation();
        op.setName("PROFILE_READ");
        op.setActive(true);
        op.setPermiteAll(false);

        when(operationRepositoryPort.findByModulePathBaseAndPathAndHttpMethod(eq("security-auth"), eq("/api/v1/profile"), eq("GET")))
                .thenReturn(Mono.just(op));

        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/profile").build());
        var auth = new UsernamePasswordAuthenticationToken("u", null, List.of(new SimpleGrantedAuthority("PROFILE_READ")));
        var context = new AuthorizationContext(exchange, null);

        StepVerifier.create(manager.authorize(Mono.just(auth), context))
                .expectNextMatches(r -> r instanceof AuthorizationDecision d && d.isGranted())
                .verifyComplete();
    }

    @Test
    void authorize_noOperation_denies() {
        when(operationRepositoryPort.findByModulePathBaseAndPathAndHttpMethod(eq("security-auth"), eq("/unknown"), eq("GET")))
                .thenReturn(Mono.empty());

        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/unknown").build());
        var context = new AuthorizationContext(exchange, null);

        StepVerifier.create(manager.authorize(Mono.empty(), context))
                .expectNextMatches(r -> r instanceof AuthorizationDecision d && !d.isGranted())
                .verifyComplete();
    }
}
