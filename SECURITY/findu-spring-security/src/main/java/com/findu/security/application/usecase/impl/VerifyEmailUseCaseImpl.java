package com.findu.security.application.usecase.impl;

import com.findu.security.application.port.output.persistence.EmailVerificationTokenRepositoryPort;
import com.findu.security.application.service.UsuarioService;
import com.findu.security.application.usecase.VerifyEmailUseCase;
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

    private final EmailVerificationTokenRepositoryPort tokenRepository;
    private final UsuarioService usuarioService;

    public VerifyEmailUseCaseImpl(EmailVerificationTokenRepositoryPort tokenRepository,
                                  UsuarioService usuarioService) {
        this.tokenRepository = tokenRepository;
        this.usuarioService = usuarioService;
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
                            .thenReturn(true)
                            .doOnSuccess(ok -> log.info("Email verified for userId={}", verificationToken.getUserId()));
                })
                .defaultIfEmpty(false);
    }
}
