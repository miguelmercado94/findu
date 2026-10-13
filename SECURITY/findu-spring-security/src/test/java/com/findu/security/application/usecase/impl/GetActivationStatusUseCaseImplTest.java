package com.findu.security.application.usecase.impl;

import com.findu.security.application.port.output.persistence.ProviderActivationRepositoryPort;
import com.findu.security.application.service.AuthenticatedUserResolver;
import com.findu.security.domain.model.ProviderActivation;
import com.findu.security.domain.model.ProviderActivationState;
import com.findu.security.domain.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetActivationStatusUseCaseImplTest {

    @Mock
    private AuthenticatedUserResolver authenticatedUserResolver;
    @Mock
    private ProviderActivationRepositoryPort activationRepository;

    private GetActivationStatusUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetActivationStatusUseCaseImpl(authenticatedUserResolver, activationRepository);
    }

    @Test
    void getStatus_returnsActivationFlags() {
        Usuario u = new Usuario();
        u.setId(42L);
        ProviderActivation a = new ProviderActivation();
        a.setUserId(42L);
        a.setEstado(ProviderActivationState.EN_REVISION_SOPORTE);
        a.setOtpOk(true);
        a.setEmailOk(true);
        a.setBiometriaOk(true);
        when(authenticatedUserResolver.resolve()).thenReturn(Mono.just(u));
        when(activationRepository.loadByUserId(42L)).thenReturn(Mono.just(a));

        StepVerifier.create(useCase.getStatus())
                .expectNextMatches(r -> "EN_REVISION_SOPORTE".equals(r.estadoActivacion())
                        && r.otpOk() && r.emailOk() && r.biometriaOk() && !r.soporteOk())
                .verifyComplete();
    }
}
