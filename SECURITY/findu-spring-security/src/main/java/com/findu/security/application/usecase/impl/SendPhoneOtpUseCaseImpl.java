package com.findu.security.application.usecase.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.findu.security.application.port.output.NotificationPort;
import com.findu.security.application.port.output.persistence.PhoneOtpStorePort;
import com.findu.security.application.service.UsuarioService;
import com.findu.security.application.usecase.SendPhoneOtpUseCase;
import com.findu.security.domain.model.PhoneOtp;
import com.findu.security.domain.model.Usuario;
import com.findu.security.util.NotificationTemplates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Genera un OTP de 6 dígitos para el teléfono del proveedor, lo persiste (hasheado) en el store
 * con TTL y lo envía por el microservicio de notificaciones (plantilla VERIFICACION_OTP).
 * Antes de enviar, vincula el teléfono y el código de país al usuario AUTENTICADO (resuelto desde
 * el contexto de seguridad), de modo que verify-otp pueda operar sobre la identidad del token.
 */
@Service
public class SendPhoneOtpUseCaseImpl implements SendPhoneOtpUseCase {
    private static final Logger log = LoggerFactory.getLogger(SendPhoneOtpUseCaseImpl.class);
    private static final String DEFAULT_COUNTRY_CODE = "+57";

    private final PhoneOtpStorePort phoneOtpStore;
    private final NotificationPort notificationPort;
    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${findu.auth.phone-otp-expiry-seconds:300}")
    private int otpExpirySeconds;

    /**
     * Código OTP fijo para pruebas locales (perfil dev). Si está vacío (qa/prod),
     * el OTP se genera aleatorio y se envía por el micro de notificaciones.
     */
    @Value("${findu.auth.dev.otp-code:}")
    private String devOtpCode;

    public SendPhoneOtpUseCaseImpl(PhoneOtpStorePort phoneOtpStore,
                                   NotificationPort notificationPort,
                                   UsuarioService usuarioService,
                                   PasswordEncoder passwordEncoder,
                                   ObjectMapper objectMapper) {
        this.phoneOtpStore = phoneOtpStore;
        this.notificationPort = notificationPort;
        this.usuarioService = usuarioService;
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
        String trimmedPhone = phone.trim();
        String e164 = country + trimmedPhone;

        return findAuthenticatedUser()
                .flatMap(user -> {
                    user.setPhone(trimmedPhone);
                    user.setCodPhoneInternational(country);
                    return usuarioService.save(user).then();
                })
                .switchIfEmpty(Mono.empty())
                .then(generateAndSend(e164, trimmedPhone, country));
    }

    private Mono<Void> generateAndSend(String e164, String trimmedPhone, String country) {
        // En dev se usa un OTP fijo (findu.auth.dev.otp-code) para no depender de SMS reales.
        boolean devMode = devOtpCode != null && !devOtpCode.isBlank();
        String plainCode = devMode ? devOtpCode.trim() : String.format("%06d", secureRandom.nextInt(1000000));
        String hashedCode = passwordEncoder.encode(plainCode);
        if (devMode) {
            log.warn("[DEV] OTP fijo activo para {} -> code={} (no se envia SMS real)", e164, plainCode);
        }
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

    /**
     * Resuelve el usuario autenticado desde el contexto reactivo si existe.
     * Si no hay autenticación válida devuelve Mono.empty() (flujo anónimo de registro).
     */
    private Mono<Usuario> findAuthenticatedUser() {
        return ReactiveSecurityContextHolder.getContext()
                .flatMap(ctx -> {
                    Authentication auth = ctx.getAuthentication();
                    if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
                        return Mono.empty();
                    }
                    Object principal = auth.getPrincipal();
                    String username;
                    if (principal instanceof Usuario) {
                        username = ((Usuario) principal).getUsername();
                    } else if (principal instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
                        username = jwt.getSubject();
                    } else {
                        return Mono.empty();
                    }
                    return usuarioService.getUserByUsername(username)
                            .switchIfEmpty(Mono.defer(() -> usuarioService.getUserByEmail(username)));
                });
    }
}
