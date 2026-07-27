package com.findu.security.presentation.controller;

import com.findu.security.application.usecase.JwtManager;
import com.findu.security.dto.response.UserProfileResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

    @Mock
    private JwtManager jwtManager;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToController(new ProfileController(jwtManager)).build();
    }

    @Test
    void profile_returnsOperationNames() {
        when(jwtManager.getCurrentUserProfile()).thenReturn(Mono.just(
                new UserProfileResponse("u1", "e@e.com", null, "+57", "ROLE_CUSTOMER", List.of("AUTH_LOGIN", "PROFILE_READ"))
        ));

        webTestClient.get()
                .uri("/api/v1/profile")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.username").isEqualTo("u1")
                .jsonPath("$.operationNames.length()").isEqualTo(2)
                .jsonPath("$.operationNames[0]").isEqualTo("AUTH_LOGIN");
    }
}
