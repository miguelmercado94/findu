package com.findu.security.application.usecase.impl;

import com.findu.security.application.port.output.EmailSenderPort;
import com.findu.security.application.port.output.NotificationPort;
import com.findu.security.application.port.output.persistence.PasswordRecoveryCodeRepositoryPort;
import com.findu.security.application.port.output.persistence.PasswordRecoveryTokenRepositoryPort;
import com.findu.security.application.service.UsuarioService;
import com.findu.security.application.usecase.RequestPasswordRecoveryUseCase;
import com.findu.security.domain.model.PasswordRecoveryCode;
import com.findu.security.domain.model.Usuario;
import com.findu.security.dto.request.ForgotPasswordRequest;
import com.findu.security.util.NotificationTemplates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class RequestPasswordRecoveryUseCaseImpl implements RequestPasswordRecoveryUseCase {
    private static final Logger log = LoggerFactory.getLogger(RequestPasswordRecoveryUseCaseImpl.class);

    private final UsuarioService usuarioService;
    private final PasswordRecoveryTokenRepositoryPort tokenRepository;
    private final PasswordRecoveryCodeRepositoryPort codeRepository;
    private final EmailSenderPort emailSender;
    private final PasswordEncoder passwordEncoder;
    private final NotificationPort notificationPort;

    @Value("${findu.auth.reset-password-base-url:http://localhost:3000/reset-password}")
    private String resetPasswordBaseUrl;

    @Value("${findu.auth.recovery-token-expiry-minutes:60}")
    private int recoveryTokenExpiryMinutes;

    @Value("${findu.auth.recovery-code-expiry-minutes:3}")
    private int recoveryCodeExpiryMinutes;

    public RequestPasswordRecoveryUseCaseImpl(UsuarioService usuarioService,
                                             PasswordRecoveryTokenRepositoryPort tokenRepository,
                                             PasswordRecoveryCodeRepositoryPort codeRepository,
                                             EmailSenderPort emailSender,
                                             PasswordEncoder passwordEncoder,
                                             NotificationPort notificationPort) {
        this.usuarioService = usuarioService;
        this.tokenRepository = tokenRepository;
        this.codeRepository = codeRepository;
        this.emailSender = emailSender;
        this.passwordEncoder = passwordEncoder;
        this.notificationPort = notificationPort;
    }

    @Override
    public Mono<Void> requestRecovery(ForgotPasswordRequest request) {
        if (request == null) {
            log.debug("Password recovery requested with empty payload");
            return Mono.empty();
        }

        Mono<Usuario> userMono;
        String logIdentifier;

        if (request.email() != null && !request.email().isBlank()) {
            String trimmedEmail = request.email().trim();
            logIdentifier = "email=" + trimmedEmail;
            userMono = usuarioService.getUserByEmail(trimmedEmail);
        } else if (request.phone() != null && !request.phone().isBlank() &&
                   request.codPhoneInternational() != null && !request.codPhoneInternational().isBlank()) {
            String trimmedPhone = request.phone().trim();
            String trimmedCountry = request.codPhoneInternational().trim();
            logIdentifier = "phone=" + trimmedCountry + " " + trimmedPhone;
            userMono = usuarioService.getUserByPhoneAndCountry(trimmedCountry, trimmedPhone);
        } else {
            log.debug("Password recovery requested without sufficient email or phone identifier");
            return Mono.empty();
        }

        log.info("Password recovery requested identifier: {}", logIdentifier);

        return userMono
                .flatMap(this::createCodeAndSendEmail)
                .then()
                .doOnSuccess(v -> log.info("Password recovery flow finished for identifier: {}", logIdentifier))
                .onErrorResume(e -> {
                    log.warn("Password recovery flow swallowed error for identifier: {} reason={}", logIdentifier, e.getMessage());
                    return Mono.empty();
                });
    }

    private Mono<Void> createCodeAndSendEmail(Usuario user) {
        log.debug("Creating 6-digit recovery code for userId={} email={}", user.getId(), user.getEmail());
        
        // El código de 6 dígitos quemado por el momento es "123456"
        String plainCode = "123456";
        String hashedCode = passwordEncoder.encode(plainCode);
        
        long now = System.currentTimeMillis();
        long expiresAt = now + (recoveryCodeExpiryMinutes * 60L * 1000L);

        PasswordRecoveryCode recoveryCode = new PasswordRecoveryCode();
        recoveryCode.setUserId(user.getId());
        recoveryCode.setCode(hashedCode);
        recoveryCode.setEmittedAt(now);
        recoveryCode.setExpiresAt(expiresAt);
        recoveryCode.setUsed(false);

        return codeRepository.save(recoveryCode)
                .doOnSuccess(saved -> log.debug("Recovery code persisted for userId={} expiresAt={}", user.getId(), expiresAt))
                .then(emailSender.sendPasswordRecoveryCode(user.getEmail(), plainCode))
                .doOnSuccess(v -> {
                    // Fire-and-forget SMS notification
                    String phone = (user.getCodPhoneInternational() != null ? user.getCodPhoneInternational() : "") +
                                   (user.getPhone() != null ? user.getPhone() : "");
                    notificationPort.send("SMS", phone, NotificationTemplates.RECUPERAR_PASSWORD, "es",
                            Map.of("user_name", user.getUsername(), "codigo", plainCode, "expiracion_minutos", String.valueOf(recoveryCodeExpiryMinutes)))
                            .subscribe();
                });
    }
}
