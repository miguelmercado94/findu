package com.findu.security.application.usecase.impl;

import com.findu.security.application.port.output.NotificationPort;
import com.findu.security.application.port.output.persistence.EmailVerificationTokenRepositoryPort;
import com.findu.security.application.service.UsuarioService;
import com.findu.security.application.usecase.SendEmailVerificationUseCase;
import com.findu.security.domain.model.EmailVerificationToken;
import com.findu.security.domain.model.Usuario;
import com.findu.security.util.NotificationTemplates;
import com.findu.security.util.TokenHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;

/**
 * Genera el token del enlace mágico de verificación de correo, persiste SOLO su hash con expiración
 * y envía el enlace por el microservicio de notificaciones (plantilla VERIFICACION_EMAIL).
 */
@Service
public class SendEmailVerificationUseCaseImpl implements SendEmailVerificationUseCase {
    private static final Logger log = LoggerFactory.getLogger(SendEmailVerificationUseCaseImpl.class);
    private static final int TOKEN_BYTES = 32;

    private final UsuarioService usuarioService;
    private final EmailVerificationTokenRepositoryPort tokenRepository;
    private final NotificationPort notificationPort;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${findu.auth.email-verify-base-url:https://api-gateway-production-ff97.up.railway.app/security-auth/api/v1/auth/email/verify}")
    private String emailVerifyBaseUrl;

    @Value("${findu.auth.email-verify-expiry-hours:24}")
    private int emailVerifyExpiryHours;

    /** En dev se loguea el enlace completo (y no se depende del correo real). */
    @Value("${findu.auth.dev.log-verification-link:false}")
    private boolean devLogVerificationLink;

    public SendEmailVerificationUseCaseImpl(UsuarioService usuarioService,
                                            EmailVerificationTokenRepositoryPort tokenRepository,
                                            NotificationPort notificationPort) {
        this.usuarioService = usuarioService;
        this.tokenRepository = tokenRepository;
        this.notificationPort = notificationPort;
    }

    @Override
    public Mono<Void> sendVerification(String email) {
        if (email == null || email.isBlank()) {
            log.debug("Email verification requested with empty email");
            return Mono.empty();
        }
        String trimmedEmail = email.trim();
        return usuarioService.getUserByEmail(trimmedEmail)
                .flatMap(this::createTokenAndSend)
                .onErrorResume(e -> {
                    log.warn("Email verification flow swallowed error for email={} reason={}", trimmedEmail, e.getMessage());
                    return Mono.empty();
                });
    }

    @Override
    public Mono<Void> sendVerificationForUser(Usuario user) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            return Mono.empty();
        }
        return createTokenAndSend(user)
                .onErrorResume(e -> {
                    log.warn("Email verification (federated) swallowed error for userId={} reason={}", user.getId(), e.getMessage());
                    return Mono.empty();
                });
    }

    private Mono<Void> createTokenAndSend(Usuario user) {
        byte[] raw = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(raw);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        String tokenHash = TokenHasher.sha256Hex(rawToken);

        EmailVerificationToken token = new EmailVerificationToken();
        token.setUserId(user.getId());
        token.setTokenHash(tokenHash);
        token.setExpiresAt(LocalDateTime.now().plusHours(emailVerifyExpiryHours));
        token.setUsed(false);
        token.setCreatedAt(LocalDateTime.now());

        String separator = emailVerifyBaseUrl.contains("?") ? "&" : "?";
        String verificationLink = emailVerifyBaseUrl + separator + "token=" + rawToken;

        return tokenRepository.save(token)
                .doOnSuccess(saved -> {
                    log.info("Email verification token persisted for userId={}", user.getId());
                    if (devLogVerificationLink) {
                        log.warn("[DEV] Enlace de verificacion de correo para {} -> {}", user.getEmail(), verificationLink);
                    }
                    notificationPort.send("EMAIL", user.getEmail(), NotificationTemplates.VERIFICACION_EMAIL, "es",
                            Map.of(
                                    "provider_name", user.getUsername() != null ? user.getUsername() : "",
                                    "verification_link", verificationLink,
                                    "expiration_hours", String.valueOf(emailVerifyExpiryHours)))
                            .subscribe();
                })
                .then();
    }
}
