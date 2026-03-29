package com.findu.security.application.usecase.impl;

import com.findu.security.application.port.output.EmailSenderPort;
import com.findu.security.application.port.output.persistence.PasswordRecoveryTokenRepositoryPort;
import com.findu.security.application.service.UsuarioService;
import com.findu.security.application.usecase.RequestPasswordRecoveryUseCase;
import com.findu.security.domain.model.PasswordRecoveryToken;
import com.findu.security.domain.model.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Service
public class RequestPasswordRecoveryUseCaseImpl implements RequestPasswordRecoveryUseCase {
    private static final Logger log = LoggerFactory.getLogger(RequestPasswordRecoveryUseCaseImpl.class);

    private final UsuarioService usuarioService;
    private final PasswordRecoveryTokenRepositoryPort tokenRepository;
    private final EmailSenderPort emailSender;

    @Value("${findu.auth.reset-password-base-url:http://localhost:3000/reset-password}")
    private String resetPasswordBaseUrl;

    @Value("${findu.auth.recovery-token-expiry-minutes:60}")
    private int recoveryTokenExpiryMinutes;

    public RequestPasswordRecoveryUseCaseImpl(UsuarioService usuarioService,
                                             PasswordRecoveryTokenRepositoryPort tokenRepository,
                                             EmailSenderPort emailSender) {
        this.usuarioService = usuarioService;
        this.tokenRepository = tokenRepository;
        this.emailSender = emailSender;
    }

    @Override
    public Mono<Void> requestRecovery(String emailOrUsername) {
        if (emailOrUsername == null || emailOrUsername.isBlank()) {
            log.debug("Password recovery requested with empty identifier");
            return Mono.empty();
        }
        String trimmed = emailOrUsername.trim();
        log.info("Password recovery requested identifier={}", trimmed);
        Mono<Usuario> userMono = trimmed.contains("@")
                ? usuarioService.getUserByEmail(trimmed)
                : usuarioService.getUserByUsername(trimmed);

        return userMono
                .flatMap(this::createTokenAndSendEmail)
                .then()
                .doOnSuccess(v -> log.info("Password recovery flow finished identifier={}", trimmed))
                .onErrorResume(e -> {
                    log.warn("Password recovery flow swallowed error identifier={} reason={}", trimmed, e.getMessage());
                    return Mono.empty();
                });
    }

    private Mono<Void> createTokenAndSendEmail(Usuario user) {
        log.debug("Creating recovery token userId={} email={}", user.getId(), user.getEmail());
        String token = UUID.randomUUID().toString().replace("-", "");
        Instant expiresAt = Instant.now().plusSeconds(recoveryTokenExpiryMinutes * 60L);

        PasswordRecoveryToken recoveryToken = new PasswordRecoveryToken();
        recoveryToken.setUserId(user.getId());
        recoveryToken.setToken(token);
        recoveryToken.setExpiresAt(expiresAt);
        recoveryToken.setUsed(false);

        String sep = resetPasswordBaseUrl.contains("?") ? "&" : "?";
        String resetLinkUrl = resetPasswordBaseUrl + sep + "token=" + token;

        return tokenRepository.save(recoveryToken)
                .doOnSuccess(saved -> log.debug("Recovery token persisted userId={} expiresAt={}", user.getId(), expiresAt))
                .then(emailSender.sendPasswordRecoveryEmail(user.getEmail(), resetLinkUrl));
    }
}
