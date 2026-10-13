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

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApproveProviderUseCaseImplTest {

    @Mock
    private ProviderActivationRepositoryPort activationRepository;
    @Mock
    private NotificationPort notificationPort;

    private ApproveProviderUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new ApproveProviderUseCaseImpl(activationRepository, notificationPort);
    }

    private ProviderActivation activation(ProviderActivationState estado) {
        ProviderActivation a = new ProviderActivation();
        a.setUserId(42L);
        a.setEstado(estado);
        return a;
    }

    @Test
    void approve_wrongState_409() {
        when(activationRepository.loadByUserId(42L))
                .thenReturn(Mono.just(activation(ProviderActivationState.PENDIENTE_VALIDACIONES)));

        StepVerifier.create(useCase.approve(42L))
                .expectErrorMatches(e -> e instanceof ResponseStatusException rse
                        && rse.getStatusCode() == HttpStatus.CONFLICT)
                .verify();

        verify(activationRepository, never()).updateUserActivationState(any(), any(), org.mockito.ArgumentMatchers.anyBoolean(), any());
    }

    @Test
    void approve_notFound_404() {
        when(activationRepository.loadByUserId(99L)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.approve(99L))
                .expectErrorMatches(e -> e instanceof ResponseStatusException rse
                        && rse.getStatusCode() == HttpStatus.NOT_FOUND)
                .verify();
    }

    @Test
    void approve_valid_marksActivated_dispatches() {
        when(activationRepository.loadByUserId(42L))
                .thenReturn(Mono.just(activation(ProviderActivationState.EN_REVISION_SOPORTE)))
                .thenReturn(Mono.just(activation(ProviderActivationState.ACTIVADO)));
        when(activationRepository.upsertValidation(eq(42L), any(), any(), any())).thenReturn(Mono.just(activation(ProviderActivationState.EN_REVISION_SOPORTE)));
        when(activationRepository.updateUserActivationState(eq(42L), eq("ACTIVADO"), org.mockito.ArgumentMatchers.anyBoolean(), any()))
                .thenReturn(Mono.empty());
        when(notificationPort.send(eq("NOTIFICACION"), eq("PROVEEDOR_42"), eq("PROVEEDOR_ACTIVADO"), eq("es"), anyMap()))
                .thenReturn(Mono.empty());

        StepVerifier.create(useCase.approve(42L))
                .expectNextMatches(r -> "ACTIVADO".equals(r.estadoActivacion()))
                .verifyComplete();

        verify(notificationPort).send(eq("NOTIFICACION"), eq("PROVEEDOR_42"), eq("PROVEEDOR_ACTIVADO"), eq("es"), eq(Map.of()));
    }
}
