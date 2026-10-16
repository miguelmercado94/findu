package com.findu.security.application.usecase.impl;

import com.findu.security.application.port.output.externalapi.ValidacionResolvePort;
import com.findu.security.application.port.output.persistence.EmailVerificationTokenRepositoryPort;
import com.findu.security.application.port.output.persistence.ProviderActivationRepositoryPort;
import com.findu.security.application.service.UsuarioService;
import com.findu.security.application.usecase.ActivationProgressService;
import com.findu.security.application.usecase.VerifyEmailUseCase;
import com.findu.security.domain.model.ValidationState;
import com.findu.security.domain.model.ValidationType;
import com.findu.security.util.TokenHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Valida el token del enlace mágico de verificación de correo. Si es válido, marca
 * email_verified=true, el token como usado y avanza account_status PENDING_EMAIL_VERIFICATION -> DOCUMENTS_PENDING.
 */
@Service
public class VerifyEmailUseCaseImpl implements VerifyEmailUseCase {
    private static final Logger log = LoggerFactory.getLogger(VerifyEmailUseCaseImpl.class);
    private static final String STATUS_PENDING_EMAIL = "PENDING_EMAIL_VERIFICATION";
    private static final String STATUS_DOCUMENTS_PENDING = "DOCUMENTS_PENDING";
    private static final String VALIDATION_CODE_EMAIL = "EMAIL_VERIFICATION";
    private static final String NOTA_EMAIL_VERIFIED = "auto:email-verified";

    private final EmailVerificationTokenRepositoryPort tokenRepository;
    private final UsuarioService usuarioService;
    private final ProviderActivationRepositoryPort activationRepository;
    private final ActivationProgressService activationProgressService;
    private final ValidacionResolvePort validacionResolvePort;

    public VerifyEmailUseCaseImpl(EmailVerificationTokenRepositoryPort tokenRepository,
                                  UsuarioService usuarioService,
                                  ProviderActivationRepositoryPort activationRepository,
                                  ActivationProgressService activationProgressService,
                                  ValidacionResolvePort validacionResolvePort) {
        this.tokenRepository = tokenRepository;
        this.usuarioService = usuarioService;
        this.activationRepository = activationRepository;
        this.activationProgressService = activationProgressService;
        this.validacionResolvePort = validacionResolvePort;
    }

    @Override
    public Mono<Boolean> verify(String token) {
        if (token == null || token.isBlank()) {
            return Mono.just(false);
        }
        String tokenHash = TokenHasher.sha256Hex(token.trim());
        return tokenRepository.findByTokenHash(tokenHash)
                .flatMap(verificationToken -> {
                    if (verificationToken.isUsed()) {
                        log.debug("Email verification token already used id={}", verificationToken.getId());
                        return Mono.just(false);
                    }
                    if (verificationToken.getExpiresAt() == null
                            || verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
                        log.debug("Email verification token expired id={}", verificationToken.getId());
                        return Mono.just(false);
                    }
                    return usuarioService.findById(verificationToken.getUserId())
                            .flatMap(user -> {
                                user.setEmailVerified(true);
                                if (STATUS_PENDING_EMAIL.equals(user.getAccountStatus())) {
                                    user.setAccountStatus(STATUS_DOCUMENTS_PENDING);
                                }
                                return usuarioService.save(user);
                            })
                            .then(tokenRepository.markUsed(verificationToken.getId()))
                            .then(markEmailOkAndRecompute(verificationToken.getUserId()))
                            .thenReturn(true)
                            .doOnSuccess(ok -> log.info("Email verified for userId={}", verificationToken.getUserId()));
                })
                .defaultIfEmpty(false);
    }

    /**
     * Marca la validación EMAIL como OK (fila versionada), recomputa el estado de activación
     * y notifica a findu-validacion (PASSED) para desactivar la alerta en vivo.
     * No-fatal: un fallo aquí no revierte la verificación de correo ya persistida.
     */
    private Mono<Void> markEmailOkAndRecompute(Long userId) {
        return activationRepository.upsertValidation(userId, ValidationType.EMAIL, ValidationState.OK, null)
                .then(activationProgressService.recompute(userId))
                .then(validacionResolvePort.markPassed(userId, VALIDATION_CODE_EMAIL, NOTA_EMAIL_VERIFIED))
                .onErrorResume(e -> {
                    log.warn("No se pudo recomputar activación tras verificar correo (userId={}): {}",
                            userId, e.getMessage());
                    return Mono.empty();
                })
                .then();
    }
}
