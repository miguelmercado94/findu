package com.findu.security.application.usecase.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.findu.security.application.port.output.persistence.PhoneOtpStorePort;
import com.findu.security.application.service.UsuarioService;
import com.findu.security.domain.model.PhoneOtp;
import com.findu.security.domain.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerifyPhoneOtpUseCaseImplTest {

    @Mock
    private PhoneOtpStorePort phoneOtpStore;
    @Mock
    private UsuarioService usuarioService;
    @Mock
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private VerifyPhoneOtpUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new VerifyPhoneOtpUseCaseImpl(phoneOtpStore, usuarioService, passwordEncoder, objectMapper);
    }

    private String otpJson(long expiresAt) throws Exception {
        return objectMapper.writeValueAsString(new PhoneOtp("hashed", expiresAt, 0));
    }

    @Test
    void verifyOtp_blank_errors() {
        StepVerifier.create(useCase.verifyOtp("", "123456", "+57"))
                .expectError(ResponseStatusException.class)
                .verify();
    }

    @Test
    void verifyOtp_notFound_errors() {
        when(phoneOtpStore.find("+573003763301")).thenReturn(Mono.empty());
        StepVerifier.create(useCase.verifyOtp("3003763301", "123456", "+57"))
                .expectError(ResponseStatusException.class)
                .verify();
    }

    @Test
    void verifyOtp_expired_deletesAndErrors() throws Exception {
        when(phoneOtpStore.find("+573003763301"))
                .thenReturn(Mono.just(otpJson(Instant.now().getEpochSecond() - 10)));
        when(phoneOtpStore.delete("+573003763301")).thenReturn(Mono.empty());

        StepVerifier.create(useCase.verifyOtp("3003763301", "123456", "+57"))
                .expectError(ResponseStatusException.class)
                .verify();
    }

    @Test
    void verifyOtp_wrongCode_errors() throws Exception {
        when(phoneOtpStore.find("+573003763301"))
                .thenReturn(Mono.just(otpJson(Instant.now().getEpochSecond() + 300)));
        when(passwordEncoder.matches("000000", "hashed")).thenReturn(false);

        StepVerifier.create(useCase.verifyOtp("3003763301", "000000", "+57"))
                .expectError(ResponseStatusException.class)
                .verify();
    }

    @Test
    void verifyOtp_success_advancesStatus() throws Exception {
        when(phoneOtpStore.find("+573003763301"))
                .thenReturn(Mono.just(otpJson(Instant.now().getEpochSecond() + 300)));
        when(passwordEncoder.matches("123456", "hashed")).thenReturn(true);

        Usuario user = new Usuario();
        user.setId(1L);
        user.setPhone("3003763301");
        user.setCodPhoneInternational("+57");
        user.setAccountStatus("PENDING_PHONE_VERIFICATION");

        when(usuarioService.getUserByPhoneAndCountry("+57", "3003763301")).thenReturn(Mono.just(user));
        when(usuarioService.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(phoneOtpStore.delete("+573003763301")).thenReturn(Mono.empty());

        StepVerifier.create(useCase.verifyOtp("3003763301", "123456", "+57"))
                .expectNextMatches(r -> r.phoneVerified()
                        && "PENDING_EMAIL_VERIFICATION".equals(r.accountStatus()))
                .verifyComplete();
    }

    @Test
    void verifyOtp_userNotFound_errors() throws Exception {
        when(phoneOtpStore.find("+573003763301"))
                .thenReturn(Mono.just(otpJson(Instant.now().getEpochSecond() + 300)));
        when(passwordEncoder.matches(anyString(), eq("hashed"))).thenReturn(true);
        when(usuarioService.getUserByPhoneAndCountry("+57", "3003763301")).thenReturn(Mono.empty());

        StepVerifier.create(useCase.verifyOtp("3003763301", "123456", "+57"))
                .expectError(ResponseStatusException.class)
                .verify();
    }
}
