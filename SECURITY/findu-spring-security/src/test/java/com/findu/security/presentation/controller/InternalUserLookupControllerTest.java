package com.findu.security.presentation.controller;

import com.findu.security.application.usecase.InternalUserManager;
import com.findu.security.dto.internal.InternalUserLookupResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InternalUserLookupControllerTest {

    private static final String INTERNAL_KEY = "findu-internal-saga-secret-key-2026";

    private InternalUserManager internalUserManager;
    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        internalUserManager = mock(InternalUserManager.class);
        InternalUserController controller = new InternalUserController(internalUserManager);
        ReflectionTestUtils.setField(controller, "expectedInternalKey", INTERNAL_KEY);
        webTestClient = WebTestClient.bindToController(controller).build();
    }

    @Test
    @DisplayName("GET /internal/users/by-username/{username} con service-key válido devuelve 200 con cuerpo")
    void lookupByUsername_withValidKey_returns200() {
        when(internalUserManager.lookupByUsername("juan"))
                .thenReturn(Mono.just(new InternalUserLookupResponse(10L, "juan", 77L)));

        webTestClient.get()
                .uri("/internal/users/by-username/{username}", "juan")
                .header("X-Internal-Service-Key", INTERNAL_KEY)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.authUserId").isEqualTo(10)
                .jsonPath("$.username").isEqualTo("juan")
                .jsonPath("$.proveedorId").isEqualTo(77);
    }

    @Test
    @DisplayName("GET /internal/users/by-username/{username} sin service-key devuelve 403")
    void lookupByUsername_withoutKey_returns403() {
        webTestClient.get()
                .uri("/internal/users/by-username/{username}", "juan")
                .exchange()
                .expectStatus().isForbidden();
    }
}
