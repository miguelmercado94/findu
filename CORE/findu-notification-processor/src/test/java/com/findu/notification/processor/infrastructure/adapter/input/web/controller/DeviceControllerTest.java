package com.findu.notification.processor.infrastructure.adapter.input.web.controller;

import com.findu.notification.processor.application.service.DeviceTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DeviceControllerTest {

    private DeviceTokenService deviceTokenService;
    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        deviceTokenService = mock(DeviceTokenService.class);
        webTestClient = WebTestClient.bindToController(new DeviceController(deviceTokenService)).build();
    }

    @Test
    @DisplayName("POST /register devuelve 200 y delega en el servicio con username/deviceToken/platform")
    void register_returns200AndDelegates() {
        webTestClient.post()
                .uri("/api/v1/devices/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "username", "juan",
                        "deviceToken", "tok-123",
                        "platform", "ANDROID"))
                .exchange()
                .expectStatus().isOk();

        verify(deviceTokenService).registerToken("juan", "tok-123", "ANDROID");
    }

    @Test
    @DisplayName("DELETE /unregister devuelve 204 y delega en el servicio con ?username=&deviceToken=")
    void unregister_returns204AndDelegates() {
        webTestClient.delete()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/devices/unregister")
                        .queryParam("username", "juan")
                        .queryParam("deviceToken", "tok-123")
                        .build())
                .exchange()
                .expectStatus().isNoContent();

        verify(deviceTokenService).unregister("juan", "tok-123");
    }
}
