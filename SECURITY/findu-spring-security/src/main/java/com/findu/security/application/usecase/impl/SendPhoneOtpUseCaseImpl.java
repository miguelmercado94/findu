package com.findu.security.application.usecase.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.findu.security.application.port.output.NotificationPort;
import com.findu.security.application.port.output.persistence.PhoneOtpStorePort;
import com.findu.security.application.usecase.SendPhoneOtpUseCase;
import com.findu.security.domain.model.PhoneOtp;
import com.findu.security.util.NotificationTemplates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Genera un OTP de 6 dígitos para el teléfono del proveedor, lo persiste (hasheado) en el store
 * con TTL y lo envía por el microservicio de notificaciones (plantilla VERIFICACION_OTP).
 */
@Service
public class SendPhoneOtpUseCaseImpl implements SendPhoneOtpUseCase {
    private static final Logger log = LoggerFactory.getLogger(SendPhoneOtpUseCaseImpl.class);
    private static final String DEFAULT_COUNTRY_CODE = "+57";

    private final PhoneOtpStorePort phoneOtpStore;
    private final NotificationPort notificationPort;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${findu.auth.phone-otp-expiry-seconds:300}")
    private int otpExpirySeconds;

    public SendPhoneOtpUseCaseImpl(PhoneOtpStorePort phoneOtpStore,
                                   NotificationPort notificationPort,
                                   PasswordEncoder passwordEncoder,
                                   ObjectMapper objectMapper) {
        this.phoneOtpStore = phoneOtpStore;
        this.notificationPort = notificationPort;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> sendOtp(String phone, String codPhoneInternational) {
        if (phone == null || phone.isBlank()) {
            log.debug("Phone OTP requested with empty phone");
            return Mono.empty();
        }
        String country = (codPhoneInternational != null && !codPhoneInternational.isBlank())
                ? codPhoneInternational.trim() : DEFAULT_COUNTRY_CODE;
        String e164 = country + phone.trim();

        String plainCode = String.format("%06d", secureRandom.nextInt(1000000));
        String hashedCode = passwordEncoder.encode(plainCode);
        long expiresAt = Instant.now().getEpochSecond() + otpExpirySeconds;

        PhoneOtp otp = new PhoneOtp(hashedCode, expiresAt, 0);
        String json;
        try {
            json = objectMapper.writeValueAsString(otp);
        } catch (Exception e) {
            log.warn("Phone OTP serialization failed for {}: {}", e164, e.getMessage());
            return Mono.error(e);
        }

        int expirationMinutes = Math.max(1, otpExpirySeconds / 60);
        return phoneOtpStore.save(e164, json, Duration.ofSeconds(otpExpirySeconds))
                .doOnSuccess(v -> {
                    log.info("Phone OTP generated and stored for {}", e164);
                    notificationPort.send("SMS", e164, NotificationTemplates.VERIFICACION_OTP, "es",
                            Map.of("otp_code", plainCode, "expiration_minutes", String.valueOf(expirationMinutes)))
                            .subscribe();
                })
                .then();
    }
}
