package com.findu.security.application.usecase.impl;

import com.findu.security.application.port.output.persistence.PasswordRecoveryTokenRepositoryPort;
import com.findu.security.application.port.output.persistence.UsuarioRepositoryPort;
import com.findu.security.application.usecase.ResetPasswordUseCase;
import com.findu.security.domain.model.PasswordRecoveryToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Implementación del caso de uso de restablecimiento de contraseña con token.
 */
@Service
public class ResetPasswordUseCaseImpl implements ResetPasswordUseCase {
    private static final Logger log = LoggerFactory.getLogger(ResetPasswordUseCaseImpl.class);

    private final PasswordRecoveryTokenRepositoryPort tokenRepository;
    private final UsuarioRepositoryPort usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public ResetPasswordUseCaseImpl(PasswordRecoveryTokenRepositoryPort tokenRepository,
                                   UsuarioRepositoryPort usuarioRepository,
                                   PasswordEncoder passwordEncoder) {
        this.tokenRepository = tokenRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Mono<Void> resetPassword(String token, String newPassword) {
        log.info("Reset password requested");
        if (token == null || token.isBlank() || newPassword == null || newPassword.isBlank()) {
            return Mono.error(new IllegalArgumentException("Token y nueva contraseña son requeridos"));
        }

        String pwd = newPassword.trim();
        return tokenRepository.findByToken(token.trim())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Token de recuperación inválido")))
                .flatMap(t -> validateAndReset(t, pwd));
    }

    private Mono<Void> validateAndReset(PasswordRecoveryToken recoveryToken, String newPassword) {
        log.debug("Validating recovery token userId={} used={}", recoveryToken.getUserId(), recoveryToken.isUsed());
        if (recoveryToken.isUsed()) {
            return Mono.error(new IllegalArgumentException("El token ya fue utilizado"));
        }
        if (recoveryToken.getExpiresAt() == null || recoveryToken.getExpiresAt().isBefore(Instant.now())) {
            return Mono.error(new IllegalArgumentException("El token ha expirado"));
        }

        String encodedPassword = passwordEncoder.encode(newPassword);
        return usuarioRepository.updatePassword(recoveryToken.getUserId(), encodedPassword)
                .doOnSuccess(v -> log.info("Password updated userId={}", recoveryToken.getUserId()))
                .then(tokenRepository.markAsUsed(recoveryToken.getToken()));
    }
}
