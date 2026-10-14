package com.findu.security.application.usecase.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.findu.security.application.port.output.persistence.PhoneOtpStorePort;
import com.findu.security.application.port.output.persistence.ProviderActivationRepositoryPort;
import com.findu.security.application.service.UsuarioService;
import com.findu.security.application.usecase.SendEmailVerificationUseCase;
import com.findu.security.application.usecase.VerifyPhoneOtpUseCase;
import com.findu.security.domain.model.PhoneOtp;
import com.findu.security.domain.model.Usuario;
import com.findu.security.domain.model.ValidationState;
import com.findu.security.domain.model.ValidationType;
import com.findu.security.dto.response.PhoneVerificationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Valida el OTP del teléfono del proveedor contra el store. Si es correcto resuelve al usuario
 * AUTENTICADO (desde el contexto de seguridad, NO por teléfono), marca phoneVerified=true y avanza
 * account_status PENDING_PHONE_VERIFICATION -> PENDING_EMAIL_VERIFICATION.
 */
@Service
public class VerifyPhoneOtpUseCaseImpl implements VerifyPhoneOtpUseCase {
    private static final Logger log = LoggerFactory.getLogger(VerifyPhoneOtpUseCaseImpl.class);
    private static final String DEFAULT_COUNTRY_CODE = "+57";
    private static final String STATUS_PENDING_PHONE = "PENDING_PHONE_VERIFICATION";
    private static final String STATUS_PENDING_EMAIL = "PENDING_EMAIL_VERIFICATION";

    private final PhoneOtpStorePort phoneOtpStore;
    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final ProviderActivationRepositoryPort activationRepository;
    private final SendEmailVerificationUseCase sendEmailVerificationUseCase;

    public VerifyPhoneOtpUseCaseImpl(PhoneOtpStorePort phoneOtpStore,
                                     UsuarioService usuarioService,
                                     PasswordEncoder passwordEncoder,
                                     ObjectMapper objectMapper,
                                     ProviderActivationRepositoryPort activationRepository,
                                     SendEmailVerificationUseCase sendEmailVerificationUseCase) {
        this.phoneOtpStore = phoneOtpStore;
        this.usuarioService = usuarioService;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
        this.activationRepository = activationRepository;
        this.sendEmailVerificationUseCase = sendEmailVerificationUseCase;
    }

    @Override
    public Mono<PhoneVerificationResponse> verifyOtp(String phone, String code, String codPhoneInternational) {
        if (phone == null || phone.isBlank() || code == null || code.isBlank()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Teléfono y código son obligatorios"));
        }
        String country = (codPhoneInternational != null && !codPhoneInternational.isBlank())
                ? codPhoneInternational.trim() : DEFAULT_COUNTRY_CODE;
        String trimmedPhone = phone.trim();
        String e164 = country + trimmedPhone;

        return phoneOtpStore.find(e164)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código inválido o expirado")))
                .flatMap(json -> {
                    PhoneOtp otp;
                    try {
                        otp = objectMapper.readValue(json, PhoneOtp.class);
                    } catch (Exception e) {
                        log.warn("Phone OTP deserialization failed for {}: {}", e164, e.getMessage());
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código inválido o expirado"));
                    }
                    if (Instant.now().getEpochSecond() > otp.expiresAtEpochSeconds()) {
                        return phoneOtpStore.delete(e164)
                                .then(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código inválido o expirado")));
                    }
                    if (!passwordEncoder.matches(code.trim(), otp.codeHash())) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código inválido o expirado"));
                    }
                    return findAuthenticatedUser()
                            .flatMap(user -> {
                                // La identidad SIEMPRE sale del token. Validamos coherencia con el
                                // teléfono del request: si el usuario ya tiene uno distinto, se rechaza.
                                String storedPhone = user.getPhone();
                                if (storedPhone != null && !storedPhone.isBlank() && !storedPhone.equals(trimmedPhone)) {
                                    return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                            "El teléfono no coincide con el usuario autenticado"));
                                }
                                // Red de seguridad para el flujo federado: send-otp acaba de setear el
                                // teléfono; si llegara en blanco lo persistimos aquí.
                                if (storedPhone == null || storedPhone.isBlank()) {
                                    user.setPhone(trimmedPhone);
                                    user.setCodPhoneInternational(country);
                                }
                                user.setPhoneVerified(true);
                                if (STATUS_PENDING_PHONE.equals(user.getAccountStatus())) {
                                    user.setAccountStatus(STATUS_PENDING_EMAIL);
                                }
                                return usuarioService.save(user)
                                        .flatMap(saved -> phoneOtpStore.delete(e164)
                                                .then(phoneOtpStore.markPhoneVerified(e164, java.time.Duration.ofMinutes(30)))
                                                .then(markOtpOk(saved))
                                                .then(sendEmailVerificationUseCase.sendVerificationForUser(saved)
                                                        .onErrorResume(e -> {
                                                            log.warn("Email de verificación no enviado tras OTP (userId={}): {}",
                                                                    saved.getId(), e.getMessage());
                                                            return Mono.empty();
                                                        }))
                                                .thenReturn(new PhoneVerificationResponse(saved.isPhoneVerified(), saved.getAccountStatus())));
                            })
                            .switchIfEmpty(Mono.defer(() -> {
                                log.info("Phone OTP verified anonymously for {}", e164);
                                return phoneOtpStore.delete(e164)
                                        .then(phoneOtpStore.markPhoneVerified(e164, java.time.Duration.ofMinutes(30)))
                                        .thenReturn(new PhoneVerificationResponse(true, "PENDING_REGISTRATION"));
                            }));
                });
    }

    /**
     * Marca la validación OTP como OK (fila versionada en provider_activation_validation). No-fatal:
     * un fallo de persistencia no bloquea el éxito del OTP.
     */
    private Mono<Void> markOtpOk(Usuario saved) {
        return activationRepository.upsertValidation(saved.getId(), ValidationType.OTP, ValidationState.OK, null)
                .onErrorResume(e -> {
                    log.warn("No se pudo marcar OTP OK (userId={}): {}", saved.getId(), e.getMessage());
                    return Mono.empty();
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
