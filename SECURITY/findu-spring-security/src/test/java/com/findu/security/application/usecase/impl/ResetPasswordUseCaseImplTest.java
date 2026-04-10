package com.findu.security.application.usecase.impl;

import com.findu.security.application.port.output.persistence.PasswordRecoveryTokenRepositoryPort;
import com.findu.security.application.port.output.persistence.UsuarioRepositoryPort;
import com.findu.security.domain.model.PasswordRecoveryToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResetPasswordUseCaseImplTest {

    @Mock
    private PasswordRecoveryTokenRepositoryPort tokenRepository;
    @Mock
    private UsuarioRepositoryPort usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private ResetPasswordUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new ResetPasswordUseCaseImpl(tokenRepository, usuarioRepository, passwordEncoder);
    }

    @Test
    void resetPassword_blank_errors() {
        StepVerifier.create(useCase.resetPassword("", "pwd"))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void resetPassword_success() {
        PasswordRecoveryToken t = new PasswordRecoveryToken();
        t.setToken("tok");
        t.setUserId(1L);
        t.setUsed(false);
        t.setExpiresAt(Instant.now().plusSeconds(3600));

        when(tokenRepository.findByToken("tok")).thenReturn(Mono.just(t));
        when(passwordEncoder.encode("new")).thenReturn("hash");
        when(usuarioRepository.updatePassword(1L, "hash")).thenReturn(Mono.empty());
        when(tokenRepository.markAsUsed("tok")).thenReturn(Mono.empty());

        StepVerifier.create(useCase.resetPassword("tok", "new"))
                .verifyComplete();
    }
}
