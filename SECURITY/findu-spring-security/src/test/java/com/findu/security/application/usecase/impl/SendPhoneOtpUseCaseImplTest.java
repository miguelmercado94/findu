package com.findu.security.application.usecase.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.findu.security.application.port.output.NotificationPort;
import com.findu.security.application.port.output.persistence.PhoneOtpStorePort;
import com.findu.security.application.service.UsuarioService;
import com.findu.security.domain.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SendPhoneOtpUseCaseImplTest {

    @Mock
    private PhoneOtpStorePort phoneOtpStore;
    @Mock
    private NotificationPort notificationPort;
    @Mock
    private UsuarioService usuarioService;
    @Mock
    private PasswordEncoder passwordEncoder;

    private SendPhoneOtpUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new SendPhoneOtpUseCaseImpl(phoneOtpStore, notificationPort, usuarioService, passwordEncoder, new ObjectMapper());
        ReflectionTestUtils.setField(useCase, "otpExpirySeconds", 300);
    }

    private SecurityContext authContext(Usuario user) {
        SecurityContext ctx = new SecurityContextImpl();
        ctx.setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
        return ctx;
    }

    private Usuario authenticatedUser() {
        Usuario user = new Usuario();
        user.setId(1L);
        user.setUsername("provider1");
        return user;
    }

    @Test
    void sendOtp_emptyPhone_returnsEmpty() {
        StepVerifier.create(useCase.sendOtp("  ", "+57"))
                .verifyComplete();
    }

    @Test
    void sendOtp_persistsPhoneOnUserAndNotifies() {
        Usuario user = authenticatedUser();
        when(usuarioService.getUserByUsername("provider1")).thenReturn(Mono.just(user));
        when(usuarioService.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_code");
        when(phoneOtpStore.save(eq("+573003763301"), anyString(), any(Duration.class))).thenReturn(Mono.empty());
        when(notificationPort.send(any(), any(), any(), any(), any())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.sendOtp("3003763301", "+57")
                        .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(authContext(user)))))
                .verifyComplete();

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioService).save(captor.capture());
        assertThat(captor.getValue().getPhone()).isEqualTo("3003763301");
        assertThat(captor.getValue().getCodPhoneInternational()).isEqualTo("+57");
        verify(phoneOtpStore).save(eq("+573003763301"), anyString(), any(Duration.class));
    }

    @Test
    void sendOtp_defaultsCountryCode() {
        Usuario user = authenticatedUser();
        when(usuarioService.getUserByUsername("provider1")).thenReturn(Mono.just(user));
        when(usuarioService.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_code");
        when(phoneOtpStore.save(eq("+573003763301"), anyString(), any(Duration.class))).thenReturn(Mono.empty());
        when(notificationPort.send(any(), any(), any(), any(), any())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.sendOtp("3003763301", null)
                        .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(authContext(user)))))
                .verifyComplete();

        verify(phoneOtpStore).save(eq("+573003763301"), anyString(), any(Duration.class));
    }

    @Test
    void sendOtp_notAuthenticated_succeedsForAnonymousRegistration() {
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_code");
        when(phoneOtpStore.save(eq("+573003763301"), anyString(), any(Duration.class))).thenReturn(Mono.empty());
        when(notificationPort.send(any(), any(), any(), any(), any())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.sendOtp("3003763301", "+57"))
                .verifyComplete();

        verify(phoneOtpStore).save(eq("+573003763301"), anyString(), any(Duration.class));
    }
}
