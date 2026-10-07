package com.findu.security.application.usecase.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.findu.security.application.port.output.NotificationPort;
import com.findu.security.application.port.output.persistence.PhoneOtpStorePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

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
    private PasswordEncoder passwordEncoder;

    private SendPhoneOtpUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new SendPhoneOtpUseCaseImpl(phoneOtpStore, notificationPort, passwordEncoder, new ObjectMapper());
        ReflectionTestUtils.setField(useCase, "otpExpirySeconds", 300);
    }

    @Test
    void sendOtp_emptyPhone_returnsEmpty() {
        StepVerifier.create(useCase.sendOtp("  ", "+57"))
                .verifyComplete();
    }

    @Test
    void sendOtp_persistsAndNotifies() {
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_code");
        when(phoneOtpStore.save(eq("+573003763301"), anyString(), any(Duration.class))).thenReturn(Mono.empty());
        when(notificationPort.send(any(), any(), any(), any(), any())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.sendOtp("3003763301", "+57"))
                .verifyComplete();

        verify(phoneOtpStore).save(eq("+573003763301"), anyString(), any(Duration.class));
    }

    @Test
    void sendOtp_defaultsCountryCode() {
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_code");
        when(phoneOtpStore.save(eq("+573003763301"), anyString(), any(Duration.class))).thenReturn(Mono.empty());
        when(notificationPort.send(any(), any(), any(), any(), any())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.sendOtp("3003763301", null))
                .verifyComplete();

        verify(phoneOtpStore).save(eq("+573003763301"), anyString(), any(Duration.class));
    }
}
