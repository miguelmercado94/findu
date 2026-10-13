package com.findu.security.application.usecase.impl;

import com.findu.security.application.port.output.externalapi.SupportTicketPort;
import com.findu.security.application.port.output.persistence.ProviderActivationRepositoryPort;
import com.findu.security.domain.model.ProviderActivation;
import com.findu.security.domain.model.ProviderActivationState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivationProgressServiceImplTest {

    @Mock
    private ProviderActivationRepositoryPort activationRepository;
    @Mock
    private SupportTicketPort supportTicketPort;

    private ActivationProgressServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ActivationProgressServiceImpl(activationRepository, supportTicketPort);
    }

    private ProviderActivation activation(ProviderActivationState estado, boolean otp, boolean email,
                                          boolean bio, boolean notified) {
        ProviderActivation a = new ProviderActivation();
        a.setUserId(42L);
        a.setEstado(estado);
        a.setOtpOk(otp);
        a.setEmailOk(email);
        a.setBiometriaOk(bio);
        a.setSoporteNotified(notified);
        return a;
    }

    @Test
    void recompute_allThreeComplete_firesTicket_andAdvances() {
        when(activationRepository.loadByUserId(42L))
                .thenReturn(Mono.just(activation(ProviderActivationState.PENDIENTE_VALIDACIONES, true, true, true, false)));
        when(supportTicketPort.createActivationTicket(eq(42L), any(), any(), eq("ACT-42")))
                .thenReturn(Mono.just("TCK-001"));
        when(activationRepository.updateUserActivationState(eq(42L), eq("EN_REVISION_SOPORTE"), eq(true), eq("TCK-001")))
                .thenReturn(Mono.empty());

        StepVerifier.create(service.recompute(42L))
                .expectNextMatches(a -> a.getEstado() == ProviderActivationState.EN_REVISION_SOPORTE
                        && a.isSoporteNotified() && "TCK-001".equals(a.getSoporteTicketCode()))
                .verifyComplete();

        verify(supportTicketPort).createActivationTicket(eq(42L), any(), any(), eq("ACT-42"));
    }

    @Test
    void recompute_alreadyNotified_doesNotFireTicketAgain() {
        when(activationRepository.loadByUserId(42L))
                .thenReturn(Mono.just(activation(ProviderActivationState.EN_REVISION_SOPORTE, true, true, true, true)));

        StepVerifier.create(service.recompute(42L))
                .expectNextMatches(a -> a.getEstado() == ProviderActivationState.EN_REVISION_SOPORTE)
                .verifyComplete();

        verify(supportTicketPort, never()).createActivationTicket(anyLong(), any(), any(), any());
        verify(activationRepository, never()).updateUserActivationState(anyLong(), any(), anyBoolean(), any());
    }

    @Test
    void recompute_ticketFails_advancesLocally_leavesNotifiedFalse() {
        when(activationRepository.loadByUserId(42L))
                .thenReturn(Mono.just(activation(ProviderActivationState.PENDIENTE_VALIDACIONES, true, true, true, false)));
        when(supportTicketPort.createActivationTicket(eq(42L), any(), any(), eq("ACT-42")))
                .thenReturn(Mono.error(new RuntimeException("help-v2 5xx")));
        when(activationRepository.updateUserActivationState(eq(42L), eq("EN_REVISION_SOPORTE"), eq(false), eq(null)))
                .thenReturn(Mono.empty());

        StepVerifier.create(service.recompute(42L))
                .expectNextMatches(a -> a.getEstado() == ProviderActivationState.EN_REVISION_SOPORTE
                        && !a.isSoporteNotified())
                .verifyComplete();
    }

    @Test
    void recompute_missingValidation_staysPending_noTicket() {
        when(activationRepository.loadByUserId(42L))
                .thenReturn(Mono.just(activation(ProviderActivationState.PENDIENTE_VALIDACIONES, true, false, true, false)));

        StepVerifier.create(service.recompute(42L))
                .expectNextMatches(a -> a.getEstado() == ProviderActivationState.PENDIENTE_VALIDACIONES)
                .verifyComplete();

        verify(supportTicketPort, never()).createActivationTicket(anyLong(), any(), any(), any());
    }

    @Test
    void recompute_alreadyActivated_noOp() {
        when(activationRepository.loadByUserId(42L))
                .thenReturn(Mono.just(activation(ProviderActivationState.ACTIVADO, true, true, true, true)));

        StepVerifier.create(service.recompute(42L))
                .expectNextMatches(a -> a.getEstado() == ProviderActivationState.ACTIVADO)
                .verifyComplete();

        verify(supportTicketPort, never()).createActivationTicket(anyLong(), any(), any(), any());
    }
}
