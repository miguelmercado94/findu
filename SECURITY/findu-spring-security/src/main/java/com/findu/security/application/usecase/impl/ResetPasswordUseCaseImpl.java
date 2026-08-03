package com.findu.security.application.usecase.impl;

import com.findu.security.application.port.output.NotificationPort;
import com.findu.security.application.port.output.persistence.PasswordRecoveryCodeRepositoryPort;
import com.findu.security.application.port.output.persistence.PasswordRecoveryTokenRepositoryPort;
import com.findu.security.application.port.output.persistence.UsuarioRepositoryPort;
import com.findu.security.application.service.UsuarioService;
import com.findu.security.application.usecase.ResetPasswordUseCase;
import com.findu.security.domain.model.PasswordRecoveryToken;
import com.findu.security.domain.model.Usuario;
import com.findu.security.dto.request.ResetPasswordRequest;
import com.findu.security.util.NotificationTemplates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class ResetPasswordUseCaseImpl implements ResetPasswordUseCase {
    private static final Logger log = LoggerFactory.getLogger(ResetPasswordUseCaseImpl.class);

    private final PasswordRecoveryTokenRepositoryPort tokenRepository;
    private final PasswordRecoveryCodeRepositoryPort codeRepository;
    private final UsuarioRepositoryPort usuarioRepository;
    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;
    private final NotificationPort notificationPort;

    public ResetPasswordUseCaseImpl(PasswordRecoveryTokenRepositoryPort tokenRepository,
                                   PasswordRecoveryCodeRepositoryPort codeRepository,
                                   UsuarioRepositoryPort usuarioRepository,
                                   UsuarioService usuarioService,
                                   PasswordEncoder passwordEncoder,
                                   NotificationPort notificationPort) {
        this.tokenRepository = tokenRepository;
        this.codeRepository = codeRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioService = usuarioService;
        this.passwordEncoder = passwordEncoder;
        this.notificationPort = notificationPort;
    }

    @Override
    public Mono<Void> resetPassword(ResetPasswordRequest request) {
        log.info("Reset password requested");
        if (request == null || request.newPassword() == null || request.newPassword().isBlank()) {
            return Mono.error(new IllegalArgumentException("Nueva contraseña es requerida"));
        }

        String newPwd = request.newPassword().trim();

        if (request.token() != null && !request.token().isBlank()) {
            // Flujo original por token (enlace)
            return tokenRepository.findByToken(request.token().trim())
                    .switchIfEmpty(Mono.error(new IllegalArgumentException("Token de recuperación inválido")))
                    .flatMap(t -> validateAndResetWithToken(t, newPwd));
        } else if (request.code() != null && !request.code().isBlank()) {
            // Flujo nuevo por código de 6 dígitos
            return findUserForCodeRecovery(request)
                    .flatMap(user -> processCodeReset(user, request.code().trim(), newPwd));
        } else {
            return Mono.error(new IllegalArgumentException("Debe proporcionar un token o un código de recuperación"));
        }
    }

    private Mono<Usuario> findUserForCodeRecovery(ResetPasswordRequest request) {
        if (request.email() != null && !request.email().isBlank()) {
            return usuarioService.getUserByEmail(request.email().trim())
                    .switchIfEmpty(Mono.error(new IllegalArgumentException("Usuario no encontrado")));
        } else if (request.phone() != null && !request.phone().isBlank() &&
                   request.codPhoneInternational() != null && !request.codPhoneInternational().isBlank()) {
            return usuarioService.getUserByPhoneAndCountry(request.codPhoneInternational().trim(), request.phone().trim())
                    .switchIfEmpty(Mono.error(new IllegalArgumentException("Usuario no encontrado")));
        } else {
            return Mono.error(new IllegalArgumentException("Debe proporcionar el email o el teléfono con código de país para validar el código"));
        }
    }

    private Mono<Void> processCodeReset(Usuario user, String plainCode, String newPassword) {
        log.debug("Processing code-based password reset for userId={}", user.getId());
        long now = System.currentTimeMillis();

        return codeRepository.findActiveByUserId(user.getId())
                .filter(c -> c.getExpiresAt() > now && passwordEncoder.matches(plainCode, c.getCode()))
                .next()
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Código de recuperación inválido o expirado")))
                .flatMap(validCode -> {
                    String encodedPassword = passwordEncoder.encode(newPassword);
                    return usuarioRepository.updatePassword(user.getId(), encodedPassword)
                            .doOnSuccess(v -> {
                                log.info("Password updated successfully for userId={} via code", user.getId());
                                // Fire-and-forget email notification
                                notificationPort.send("EMAIL", user.getEmail(), NotificationTemplates.PASSWORD_CAMBIADA, "es",
                                        Map.of("user_name", user.getUsername()))
                                        .subscribe();
                            })
                            .then(codeRepository.markAsUsed(validCode.getId()));
                });
    }

    private Mono<Void> validateAndResetWithToken(PasswordRecoveryToken recoveryToken, String newPassword) {
        log.debug("Validating recovery token userId={} used={}", recoveryToken.getUserId(), recoveryToken.isUsed());
        if (recoveryToken.isUsed()) {
            return Mono.error(new IllegalArgumentException("El token ya fue utilizado"));
        }
        if (recoveryToken.getExpiresAt() == null || recoveryToken.getExpiresAt().isBefore(java.time.Instant.now())) {
            return Mono.error(new IllegalArgumentException("El token ha expirado"));
        }

        String encodedPassword = passwordEncoder.encode(newPassword);
        return usuarioRepository.updatePassword(recoveryToken.getUserId(), encodedPassword)
                .doOnSuccess(v -> {
                    log.info("Password updated userId={}", recoveryToken.getUserId());
                    // Fire-and-forget email notification via token flow
                    usuarioService.findById(recoveryToken.getUserId())
                            .doOnNext(user -> notificationPort.send("EMAIL", user.getEmail(), NotificationTemplates.PASSWORD_CAMBIADA, "es",
                                    Map.of("user_name", user.getUsername()))
                                    .subscribe())
                            .subscribe();
                })
                .then(tokenRepository.markAsUsed(recoveryToken.getToken()));
    }
}
