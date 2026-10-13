package com.findu.security.application.usecase.impl;

import com.findu.security.application.port.output.NotificationPort;
import com.findu.security.application.port.output.persistence.ProviderActivationRepositoryPort;
import com.findu.security.domain.model.ProviderActivation;
import com.findu.security.domain.model.ProviderActivationState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RejectProviderUseCaseImplTest {

    @Mock
    private ProviderActivationRepositoryPort activationRepository;
    @Mock
    private NotificationPort notificationPort;

    private RejectProviderUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new RejectProviderUseCaseImpl(activationRepository, notificationPort);
    }

    private ProviderActivation activation(ProviderActivationState estado) {
        ProviderActivation a = new ProviderActivation();
        a.setUserId(42L);
        a.setEstado(estado);
        return a;
    }

    @Test
    void reject_wrongState_409() {
        when(activationRepository.loadByUserId(42L))
                .thenReturn(Mono.just(activation(ProviderActivationState.ACTIVADO)));

        StepVerifier.create(useCase.reject(42L, "motivo"))
                .expectErrorMatches(e -> e instanceof ResponseStatusException rse
                        && rse.getStatusCode() == HttpStatus.CONFLICT)
                .verify();
    }

    @Test
    void reject_valid_marksRejected_resetsNotified_dispatches() {
        when(activationRepository.loadByUserId(42L))
                .thenReturn(Mono.just(activation(ProviderActivationState.EN_REVISION_SOPORTE)))
                .thenReturn(Mono.just(activation(ProviderActivationState.RECHAZADO)));
        when(activationRepository.upsertValidation(eq(42L), any(), any(), eq("datos no coinciden")))
                .thenReturn(Mono.just(activation(ProviderActivationState.EN_REVISION_SOPORTE)));
        when(activationRepository.updateUserActivationState(eq(42L), eq("RECHAZADO"), eq(false), any()))
                .thenReturn(Mono.empty());
        when(notificationPort.send(eq("NOTIFICACION"), eq("PROVEEDOR_42"), eq("PROVEEDOR_RECHAZADO"), eq("es"), anyMap()))
                .thenReturn(Mono.empty());

        StepVerifier.create(useCase.reject(42L, "datos no coinciden"))
                .expectNextMatches(r -> "RECHAZADO".equals(r.estadoActivacion()))
                .verifyComplete();

        verify(activationRepository).updateUserActivationState(eq(42L), eq("RECHAZADO"), eq(false), any());
        verify(notificationPort).send(eq("NOTIFICACION"), eq("PROVEEDOR_42"), eq("PROVEEDOR_RECHAZADO"), eq("es"), anyMap());
    }
}
