package com.findu.security.application.usecase.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.findu.security.application.port.output.persistence.PhoneOtpStorePort;
import com.findu.security.application.port.output.persistence.ProviderActivationRepositoryPort;
import com.findu.security.application.service.UsuarioService;
import com.findu.security.application.usecase.SendEmailVerificationUseCase;
import com.findu.security.domain.model.PhoneOtp;
import com.findu.security.domain.model.ProviderActivation;
import com.findu.security.domain.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;

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
    @Mock
    private ProviderActivationRepositoryPort activationRepository;
    @Mock
    private SendEmailVerificationUseCase sendEmailVerificationUseCase;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private VerifyPhoneOtpUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new VerifyPhoneOtpUseCaseImpl(phoneOtpStore, usuarioService, passwordEncoder, objectMapper,
                activationRepository, sendEmailVerificationUseCase);
    }

    private String otpJson(long expiresAt) throws Exception {
        return objectMapper.writeValueAsString(new PhoneOtp("hashed", expiresAt, 0));
    }

    private SecurityContext authContext(Usuario user) {
        SecurityContext ctx = new SecurityContextImpl();
        ctx.setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
        return ctx;
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
    void verifyOtp_success_advancesStatus_usingAuthenticatedUser() throws Exception {
        when(phoneOtpStore.find("+573003763301"))
                .thenReturn(Mono.just(otpJson(Instant.now().getEpochSecond() + 300)));
        when(passwordEncoder.matches("123456", "hashed")).thenReturn(true);

        Usuario user = new Usuario();
        user.setId(1L);
        user.setUsername("provider1");
        user.setPhone("3003763301");
        user.setCodPhoneInternational("+57");
        user.setAccountStatus("PENDING_PHONE_VERIFICATION");

        when(usuarioService.getUserByUsername("provider1")).thenReturn(Mono.just(user));
        when(usuarioService.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(phoneOtpStore.delete("+573003763301")).thenReturn(Mono.empty());
        when(activationRepository.upsertValidation(any(), any(), any(), any()))
                .thenReturn(Mono.just(new ProviderActivation()));
        when(sendEmailVerificationUseCase.sendVerificationForUser(any())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.verifyOtp("3003763301", "123456", "+57")
                        .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(authContext(user)))))
                .expectNextMatches(r -> r.phoneVerified()
                        && "PENDING_EMAIL_VERIFICATION".equals(r.accountStatus()))
                .verifyComplete();
    }

    @Test
    void verifyOtp_federatedUserWithBlankPhone_persistsPhone() throws Exception {
        when(phoneOtpStore.find("+573003763301"))
                .thenReturn(Mono.just(otpJson(Instant.now().getEpochSecond() + 300)));
        when(passwordEncoder.matches("123456", "hashed")).thenReturn(true);

        Usuario user = new Usuario();
        user.setId(2L);
        user.setUsername("federated1");
        user.setPhone(null);
        user.setAccountStatus("PENDING_PHONE_VERIFICATION");

        when(usuarioService.getUserByUsername("federated1")).thenReturn(Mono.just(user));
        when(usuarioService.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(phoneOtpStore.delete("+573003763301")).thenReturn(Mono.empty());
        when(activationRepository.upsertValidation(any(), any(), any(), any()))
                .thenReturn(Mono.just(new ProviderActivation()));
        when(sendEmailVerificationUseCase.sendVerificationForUser(any())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.verifyOtp("3003763301", "123456", "+57")
                        .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(authContext(user)))))
                .expectNextMatches(r -> r.phoneVerified()
                        && "PENDING_EMAIL_VERIFICATION".equals(r.accountStatus()))
                .verifyComplete();
    }

    @Test
    void verifyOtp_phoneMismatch_errors() throws Exception {
        when(phoneOtpStore.find("+573003763301"))
                .thenReturn(Mono.just(otpJson(Instant.now().getEpochSecond() + 300)));
        when(passwordEncoder.matches(anyString(), eq("hashed"))).thenReturn(true);

        Usuario user = new Usuario();
        user.setId(3L);
        user.setUsername("provider2");
        user.setPhone("3009999999");
        user.setCodPhoneInternational("+57");
        user.setAccountStatus("PENDING_PHONE_VERIFICATION");

        when(usuarioService.getUserByUsername("provider2")).thenReturn(Mono.just(user));

        StepVerifier.create(useCase.verifyOtp("3003763301", "123456", "+57")
                        .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(authContext(user)))))
                .expectError(ResponseStatusException.class)
                .verify();
    }

    @Test
    void verifyOtp_notAuthenticated_errors() throws Exception {
        when(phoneOtpStore.find("+573003763301"))
                .thenReturn(Mono.just(otpJson(Instant.now().getEpochSecond() + 300)));
        when(passwordEncoder.matches(anyString(), eq("hashed"))).thenReturn(true);

        StepVerifier.create(useCase.verifyOtp("3003763301", "123456", "+57"))
                .expectError(ResponseStatusException.class)
                .verify();
    }
}
