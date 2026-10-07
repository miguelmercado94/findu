package com.findu.security.application.usecase.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.findu.security.application.port.output.persistence.PhoneOtpStorePort;
import com.findu.security.application.service.UsuarioService;
import com.findu.security.application.usecase.VerifyPhoneOtpUseCase;
import com.findu.security.domain.model.PhoneOtp;
import com.findu.security.domain.model.Usuario;
import com.findu.security.dto.response.PhoneVerificationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Valida el OTP del teléfono del proveedor contra el store. Si es correcto marca
 * phoneVerified=true y avanza account_status PENDING_PHONE_VERIFICATION -> PENDING_EMAIL_VERIFICATION.
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

    public VerifyPhoneOtpUseCaseImpl(PhoneOtpStorePort phoneOtpStore,
                                     UsuarioService usuarioService,
                                     PasswordEncoder passwordEncoder,
                                     ObjectMapper objectMapper) {
        this.phoneOtpStore = phoneOtpStore;
        this.usuarioService = usuarioService;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
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
                    return usuarioService.getUserByPhoneAndCountry(country, trimmedPhone)
                            .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado para el teléfono")))
                            .flatMap(user -> {
                                user.setPhoneVerified(true);
                                if (STATUS_PENDING_PHONE.equals(user.getAccountStatus())) {
                                    user.setAccountStatus(STATUS_PENDING_EMAIL);
                                }
                                return usuarioService.save(user);
                            })
                            .flatMap(saved -> phoneOtpStore.delete(e164)
                                    .thenReturn(new PhoneVerificationResponse(saved.isPhoneVerified(), saved.getAccountStatus())));
                });
    }
}
