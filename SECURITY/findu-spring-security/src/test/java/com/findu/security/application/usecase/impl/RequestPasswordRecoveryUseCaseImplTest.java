package com.findu.security.application.usecase.impl;

import com.findu.security.application.port.output.EmailSenderPort;
import com.findu.security.application.port.output.persistence.PasswordRecoveryTokenRepositoryPort;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestPasswordRecoveryUseCaseImplTest {

    @Mock
    private UsuarioService usuarioService;
    @Mock
    private PasswordRecoveryTokenRepositoryPort tokenRepository;
    @Mock
    private EmailSenderPort emailSender;

    private RequestPasswordRecoveryUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new RequestPasswordRecoveryUseCaseImpl(usuarioService, tokenRepository, emailSender);
        ReflectionTestUtils.setField(useCase, "resetPasswordBaseUrl", "http://localhost/reset");
        ReflectionTestUtils.setField(useCase, "recoveryTokenExpiryMinutes", 60);
    }

    @Test
    void requestRecovery_empty_returnsEmpty() {
        StepVerifier.create(useCase.requestRecovery("  "))
                .verifyComplete();
    }

    @Test
    void requestRecovery_sendsEmail() {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setEmail("e@e.com");
        when(usuarioService.getUserByUsername("john")).thenReturn(Mono.just(u));
        when(tokenRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(emailSender.sendPasswordRecoveryEmail(any(), any())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.requestRecovery("john"))
                .verifyComplete();
    }
}
