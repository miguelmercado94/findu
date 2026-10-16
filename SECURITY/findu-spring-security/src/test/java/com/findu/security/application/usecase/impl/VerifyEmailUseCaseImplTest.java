package com.findu.security.application.usecase.impl;

import com.findu.security.application.port.output.externalapi.ValidacionResolvePort;
import com.findu.security.application.port.output.persistence.EmailVerificationTokenRepositoryPort;
import com.findu.security.application.port.output.persistence.ProviderActivationRepositoryPort;
import com.findu.security.application.service.UsuarioService;
import com.findu.security.application.usecase.ActivationProgressService;
import com.findu.security.domain.model.EmailVerificationToken;
import com.findu.security.domain.model.ProviderActivation;
import com.findu.security.domain.model.Usuario;
import com.findu.security.util.TokenHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerifyEmailUseCaseImplTest {

    @Mock
    private EmailVerificationTokenRepositoryPort tokenRepository;
    @Mock
    private UsuarioService usuarioService;
    @Mock
    private ProviderActivationRepositoryPort activationRepository;
    @Mock
    private ActivationProgressService activationProgressService;
    @Mock
    private ValidacionResolvePort validacionResolvePort;

    private VerifyEmailUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new VerifyEmailUseCaseImpl(tokenRepository, usuarioService,
                activationRepository, activationProgressService, validacionResolvePort);
    }

    @Test
    void verify_blank_returnsFalse() {
        StepVerifier.create(useCase.verify("  "))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void verify_notFound_returnsFalse() {
        when(tokenRepository.findByTokenHash(TokenHasher.sha256Hex("tok"))).thenReturn(Mono.empty());
        StepVerifier.create(useCase.verify("tok"))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void verify_used_returnsFalse() {
        EmailVerificationToken t = new EmailVerificationToken();
        t.setId(1L);
        t.setUsed(true);
        t.setExpiresAt(LocalDateTime.now().plusHours(1));
        when(tokenRepository.findByTokenHash(TokenHasher.sha256Hex("tok"))).thenReturn(Mono.just(t));

        StepVerifier.create(useCase.verify("tok"))
                .expectNext(false)
                .verifyComplete();
        verify(usuarioService, never()).findById(anyLong());
    }

    @Test
    void verify_expired_returnsFalse() {
        EmailVerificationToken t = new EmailVerificationToken();
        t.setId(1L);
        t.setUsed(false);
        t.setExpiresAt(LocalDateTime.now().minusHours(1));
        when(tokenRepository.findByTokenHash(TokenHasher.sha256Hex("tok"))).thenReturn(Mono.just(t));

        StepVerifier.create(useCase.verify("tok"))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void verify_success_advancesStatus() {
        EmailVerificationToken t = new EmailVerificationToken();
        t.setId(5L);
        t.setUserId(10L);
        t.setUsed(false);
        t.setExpiresAt(LocalDateTime.now().plusHours(1));

        Usuario user = new Usuario();
        user.setId(10L);
        user.setAccountStatus("PENDING_EMAIL_VERIFICATION");

        when(tokenRepository.findByTokenHash(TokenHasher.sha256Hex("tok"))).thenReturn(Mono.just(t));
        when(usuarioService.findById(10L)).thenReturn(Mono.just(user));
        when(usuarioService.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(tokenRepository.markUsed(5L)).thenReturn(Mono.empty());
        ProviderActivation activation = new ProviderActivation();
        activation.setUserId(10L);
        when(activationRepository.upsertValidation(eq(10L), any(), any(), any())).thenReturn(Mono.just(activation));
        when(activationProgressService.recompute(10L)).thenReturn(Mono.just(activation));
        when(validacionResolvePort.markPassed(10L, "EMAIL_VERIFICATION", "auto:email-verified"))
                .thenReturn(Mono.empty());

        StepVerifier.create(useCase.verify("tok"))
                .expectNext(true)
                .verifyComplete();

        verify(usuarioService).save(any());
        verify(activationProgressService).recompute(10L);
        verify(validacionResolvePort).markPassed(10L, "EMAIL_VERIFICATION", "auto:email-verified");
    }
}
