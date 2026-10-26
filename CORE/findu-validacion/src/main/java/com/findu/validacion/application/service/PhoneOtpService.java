package com.findu.validacion.application.service;

import com.findu.validacion.application.dto.request.SendPhoneOtpRequest;
import com.findu.validacion.application.dto.request.VerifyPhoneOtpRequest;
import com.findu.validacion.application.dto.response.SendPhoneOtpResponse;
import com.findu.validacion.application.dto.response.VerifyPhoneOtpResponse;
import com.findu.validacion.application.port.in.ValidatePhoneOtpUseCase;
import com.findu.validacion.application.port.out.PhoneOtpRepositoryPort;
import com.findu.validacion.domain.model.PhoneOtpRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhoneOtpService implements ValidatePhoneOtpUseCase {

    private static final String DEFAULT_COUNTRY_CODE = "+57";
    public static final String DEFAULT_PROCESS_CODE = "VINCULACION_LLAVE_BREB";
    private static final int DEFAULT_EXPIRY_SECONDS = 300; // 5 minutos

    private final PhoneOtpRepositoryPort otpRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${findu.auth.dev.otp-code:123456}")
    private String devOtpCode;

    @Value("${findu.auth.phone-otp-expiry-seconds:300}")
    private int otpExpirySeconds;

    @Override
    public Mono<SendPhoneOtpResponse> sendOtp(SendPhoneOtpRequest request) {
        String country = (request.codPhoneInternational() != null && !request.codPhoneInternational().isBlank())
                ? request.codPhoneInternational().trim()
                : DEFAULT_COUNTRY_CODE;
        String trimmedPhone = request.phone().trim();
        String e164 = country + trimmedPhone;
        String processCode = (request.processCode() != null && !request.processCode().isBlank())
                ? request.processCode().trim()
                : DEFAULT_PROCESS_CODE;

        boolean devMode = devOtpCode != null && !devOtpCode.isBlank();
        String code = devMode ? devOtpCode.trim() : String.format("%06d", secureRandom.nextInt(1000000));

        if (devMode) {
            log.info("[DEV] OTP generado para {} (proceso={}): code={}", e164, processCode, code);
        }

        Instant expiresAt = Instant.now().plusSeconds(otpExpirySeconds > 0 ? otpExpirySeconds : DEFAULT_EXPIRY_SECONDS);

        PhoneOtpRecord record = new PhoneOtpRecord(
                null,
                e164,
                code,
                processCode,
                expiresAt,
                false,
                null,
                Instant.now()
        );

        return otpRepository.saveOtp(record)
                .doOnSuccess(saved -> log.info("Phone OTP guardado en MongoDB para e164={}, processCode={}", e164, processCode))
                .thenReturn(new SendPhoneOtpResponse(
                        "SENT",
                        otpExpirySeconds > 0 ? otpExpirySeconds : DEFAULT_EXPIRY_SECONDS,
                        "Código OTP enviado exitosamente al teléfono " + e164
                ));
    }

    @Override
    public Mono<VerifyPhoneOtpResponse> verifyOtp(VerifyPhoneOtpRequest request) {
        String country = (request.codPhoneInternational() != null && !request.codPhoneInternational().isBlank())
                ? request.codPhoneInternational().trim()
                : DEFAULT_COUNTRY_CODE;
        String trimmedPhone = request.phone().trim();
        String e164 = country + trimmedPhone;
        String processCode = (request.processCode() != null && !request.processCode().isBlank())
                ? request.processCode().trim()
                : DEFAULT_PROCESS_CODE;
        String inputCode = request.code().trim();

        // Soporte de OTP fijo en Dev / QA (123456)
        if (devOtpCode != null && devOtpCode.trim().equals(inputCode)) {
            log.info("[DEV] OTP validado exitosamente via código estático para {} (proceso={})", e164, processCode);
            return otpRepository.markPhoneVerified(e164, processCode, Duration.ofMinutes(30))
                    .thenReturn(new VerifyPhoneOtpResponse(true, "Código OTP verificado exitosamente"));
        }

        return otpRepository.findLatestOtp(e164, processCode)
                .flatMap(record -> {
                    if (record.isExpired()) {
                        log.warn("OTP expirado para {} (proceso={})", e164, processCode);
                        return Mono.just(new VerifyPhoneOtpResponse(false, "El código OTP ha expirado"));
                    }
                    if (!inputCode.equals(record.code())) {
                        log.warn("OTP inválido para {} (esperado={}, recibido={})", e164, record.code(), inputCode);
                        return Mono.just(new VerifyPhoneOtpResponse(false, "Código OTP inválido"));
                    }

                    return otpRepository.markPhoneVerified(e164, processCode, Duration.ofMinutes(30))
                            .thenReturn(new VerifyPhoneOtpResponse(true, "Código OTP verificado exitosamente"));
                })
                .defaultIfEmpty(new VerifyPhoneOtpResponse(false, "No se encontró un código OTP activo para este teléfono"));
    }
}
