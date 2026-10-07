package com.findu.security.application.usecase.impl;

import com.findu.security.application.port.output.NotificationPort;
import com.findu.security.application.port.output.persistence.EmailVerificationTokenRepositoryPort;
import com.findu.security.application.service.UsuarioService;
import com.findu.security.domain.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SendEmailVerificationUseCaseImplTest {

    @Mock
    private UsuarioService usuarioService;
    @Mock
    private EmailVerificationTokenRepositoryPort tokenRepository;
    @Mock
    private NotificationPort notificationPort;

    private SendEmailVerificationUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new SendEmailVerificationUseCaseImpl(usuarioService, tokenRepository, notificationPort);
        ReflectionTestUtils.setField(useCase, "emailVerifyBaseUrl", "https://example.com/verify");
        ReflectionTestUtils.setField(useCase, "emailVerifyExpiryHours", 24);
    }

    private Usuario user() {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setUsername("proveedor");
        u.setEmail("p@e.com");
        return u;
    }

    @Test
    void sendVerification_emptyEmail_returnsEmpty() {
        StepVerifier.create(useCase.sendVerification("  "))
                .verifyComplete();
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void sendVerification_userNotFound_swallows() {
        when(usuarioService.getUserByEmail("p@e.com")).thenReturn(Mono.empty());
        StepVerifier.create(useCase.sendVerification("p@e.com"))
                .verifyComplete();
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void sendVerification_persistsTokenAndNotifies() {
        when(usuarioService.getUserByEmail("p@e.com")).thenReturn(Mono.just(user()));
        when(tokenRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(notificationPort.send(any(), any(), any(), any(), any())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.sendVerification("p@e.com"))
                .verifyComplete();

        verify(tokenRepository).save(any());
    }

    @Test
    void sendVerificationForUser_null_returnsEmpty() {
        StepVerifier.create(useCase.sendVerificationForUser(null))
                .verifyComplete();
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void sendVerificationForUser_persists() {
        when(tokenRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(notificationPort.send(any(), any(), any(), any(), any())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.sendVerificationForUser(user()))
                .verifyComplete();

        verify(tokenRepository).save(any());
    }
}
