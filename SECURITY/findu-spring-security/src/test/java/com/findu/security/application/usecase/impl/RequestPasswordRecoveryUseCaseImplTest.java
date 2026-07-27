package com.findu.security.application.usecase.impl;

import com.findu.security.application.port.output.EmailSenderPort;
import com.findu.security.application.port.output.persistence.PasswordRecoveryCodeRepositoryPort;
import com.findu.security.application.port.output.persistence.PasswordRecoveryTokenRepositoryPort;
import com.findu.security.application.service.UsuarioService;
import com.findu.security.domain.model.Usuario;
import com.findu.security.dto.request.ForgotPasswordRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestPasswordRecoveryUseCaseImplTest {

    @Mock
    private UsuarioService usuarioService;
    @Mock
    private PasswordRecoveryTokenRepositoryPort tokenRepository;
    @Mock
    private PasswordRecoveryCodeRepositoryPort codeRepository;
    @Mock
    private EmailSenderPort emailSender;
    @Mock
    private PasswordEncoder passwordEncoder;

    private RequestPasswordRecoveryUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new RequestPasswordRecoveryUseCaseImpl(usuarioService, tokenRepository, codeRepository, emailSender, passwordEncoder);
        ReflectionTestUtils.setField(useCase, "resetPasswordBaseUrl", "http://localhost/reset");
        ReflectionTestUtils.setField(useCase, "recoveryTokenExpiryMinutes", 60);
        ReflectionTestUtils.setField(useCase, "recoveryCodeExpiryMinutes", 3);
    }

    @Test
    void requestRecovery_empty_returnsEmpty() {
        ForgotPasswordRequest req = new ForgotPasswordRequest("  ", null, null);
        StepVerifier.create(useCase.requestRecovery(req))
                .verifyComplete();
    }

    @Test
    void requestRecovery_sendsEmail() {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setEmail("e@e.com");
        ForgotPasswordRequest req = new ForgotPasswordRequest("e@e.com", null, null);
        
        when(usuarioService.getUserByEmail("e@e.com")).thenReturn(Mono.just(u));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_code");
        when(codeRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(emailSender.sendPasswordRecoveryCode(any(), any())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.requestRecovery(req))
                .verifyComplete();
    }
}
