package com.findu.validacion.application.service;

import com.findu.validacion.application.dto.request.RegisterBrebKeyRequest;
import com.findu.validacion.application.dto.request.UpdateBrebKeyRequest;
import com.findu.validacion.application.dto.response.ProviderBrebKeyResponse;
import com.findu.validacion.application.port.in.ManageProviderBrebKeyUseCase;
import com.findu.validacion.application.port.out.ProcessValidationInstanceRepositoryPort;
import com.findu.validacion.application.port.out.ProviderBrebKeyRepositoryPort;
import com.findu.validacion.domain.enums.ValidationMode;
import com.findu.validacion.domain.enums.ValidationStatus;
import com.findu.validacion.domain.exception.BrebKeyNotFoundException;
import com.findu.validacion.domain.exception.InvalidOtpException;
import com.findu.validacion.domain.model.ProcessValidationInstance;
import com.findu.validacion.domain.model.ProviderBrebKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderBrebKeyService implements ManageProviderBrebKeyUseCase {

    public static final String PROCESS_VINCULACION = "VINCULACION_LLAVE_BREB";
    public static final String PROCESS_CAMBIO = "CAMBIO_LLAVE_BREB";
    public static final String VALIDATION_CODE_OTP = "OTP_PHONE_SMS";
    private static final String DEFAULT_COUNTRY_CODE = "+57";

    private final ProviderBrebKeyRepositoryPort brebKeyRepo;
    private final ProcessValidationInstanceRepositoryPort instanceRepo;
    private final PhoneOtpService phoneOtpService;

    @Value("${findu.auth.dev.otp-code:123456}")
    private String devOtpCode;

    @Override
    public Mono<ProviderBrebKeyResponse> registerKey(RegisterBrebKeyRequest request) {
        log.info("Iniciando registro de llave Bre-B para providerId={}, banco={}, tipo={}",
                request.providerId(), request.bank(), request.keyType());

        return validateOtp(request.phone(), request.codPhoneInternational(), request.otpCode(), PROCESS_VINCULACION)
                .flatMap(valid -> {
                    if (!Boolean.TRUE.equals(valid)) {
                        return Mono.error(new InvalidOtpException("El código OTP proporcionado es inválido o ha expirado."));
                    }

                    // Inactivar llave previa si existe
                    return brebKeyRepo.findActiveByProviderId(request.providerId())
                            .flatMap(existing -> {
                                ProviderBrebKey inactive = new ProviderBrebKey(
                                        existing.id(),
                                        existing.providerId(),
                                        existing.keyType(),
                                        existing.keyValue(),
                                        existing.bank(),
                                        existing.phone(),
                                        existing.codPhoneInternational(),
                                        ProviderBrebKey.STATUS_INACTIVE,
                                        existing.createdAt(),
                                        Instant.now()
                                );
                                return brebKeyRepo.save(inactive);
                            })
                            .then(Mono.defer(() -> {
                                Instant now = Instant.now();
                                ProviderBrebKey newKey = new ProviderBrebKey(
                                        null,
                                        request.providerId(),
                                        request.keyType().toUpperCase(),
                                        request.keyValue().trim(),
                                        request.bank().trim(),
                                        request.phone().trim(),
                                        request.codPhoneInternational() != null ? request.codPhoneInternational().trim() : DEFAULT_COUNTRY_CODE,
                                        ProviderBrebKey.STATUS_ACTIVE,
                                        now,
                                        now
                                );
                                return brebKeyRepo.save(newKey);
                            }))
                            .flatMap(savedKey -> recordValidationInstance(
                                    request.providerId(),
                                    PROCESS_VINCULACION,
                                    Map.of(
                                            "keyType", savedKey.keyType(),
                                            "bank", savedKey.bank(),
                                            "phone", savedKey.phone(),
                                            "otpVerified", true
                                    )
                            ).thenReturn(toResponse(savedKey)));
                });
    }

    @Override
    public Mono<ProviderBrebKeyResponse> updateKey(Long providerId, UpdateBrebKeyRequest request) {
        log.info("Iniciando actualización de llave Bre-B para providerId={}, banco={}, tipo={}",
                providerId, request.bank(), request.keyType());

        return validateOtp(request.phone(), request.codPhoneInternational(), request.otpCode(), PROCESS_CAMBIO)
                .flatMap(valid -> {
                    if (!Boolean.TRUE.equals(valid)) {
                        return Mono.error(new InvalidOtpException("El código OTP proporcionado es inválido o ha expirado."));
                    }

                    return brebKeyRepo.findActiveByProviderId(providerId)
                            .switchIfEmpty(Mono.error(new BrebKeyNotFoundException(
                                    "No se encontró una llave Bre-B activa para el proveedor " + providerId)))
                            .flatMap(existing -> {
                                ProviderBrebKey updated = new ProviderBrebKey(
                                        existing.id(),
                                        existing.providerId(),
                                        request.keyType().toUpperCase(),
                                        request.keyValue().trim(),
                                        request.bank().trim(),
                                        request.phone().trim(),
                                        request.codPhoneInternational() != null ? request.codPhoneInternational().trim() : DEFAULT_COUNTRY_CODE,
                                        ProviderBrebKey.STATUS_ACTIVE,
                                        existing.createdAt(),
                                        Instant.now()
                                );
                                return brebKeyRepo.save(updated);
                            })
                            .flatMap(savedKey -> recordValidationInstance(
                                    providerId,
                                    PROCESS_CAMBIO,
                                    Map.of(
                                            "keyType", savedKey.keyType(),
                                            "bank", savedKey.bank(),
                                            "phone", savedKey.phone(),
                                            "otpVerified", true
                                    )
                            ).thenReturn(toResponse(savedKey)));
                });
    }

    @Override
    public Mono<ProviderBrebKeyResponse> getKey(Long providerId) {
        return brebKeyRepo.findActiveByProviderId(providerId)
                .switchIfEmpty(Mono.error(new BrebKeyNotFoundException(
                        "No se encontró una llave Bre-B activa para el proveedor " + providerId)))
                .map(this::toResponse);
    }

    @Override
    public Mono<Void> deactivateKey(Long providerId) {
        return brebKeyRepo.findActiveByProviderId(providerId)
                .flatMap(existing -> {
                    ProviderBrebKey inactive = new ProviderBrebKey(
                            existing.id(),
                            existing.providerId(),
                            existing.keyType(),
                            existing.keyValue(),
                            existing.bank(),
                            existing.phone(),
                            existing.codPhoneInternational(),
                            ProviderBrebKey.STATUS_INACTIVE,
                            existing.createdAt(),
                            Instant.now()
                    );
                    return brebKeyRepo.save(inactive);
                })
                .then();
    }

    private Mono<Boolean> validateOtp(String phone, String country, String code, String processCode) {
        if (code == null || code.isBlank()) {
            return Mono.just(false);
        }
        // Soporte OTP dev / QA
        if (devOtpCode != null && devOtpCode.trim().equals(code.trim())) {
            log.info("[DEV] OTP validado exitosamente via código estático 123456");
            return Mono.just(true);
        }

        var verifyReq = new com.findu.validacion.application.dto.request.VerifyPhoneOtpRequest(
                phone, country, code, processCode);
        return phoneOtpService.verifyOtp(verifyReq)
                .map(com.findu.validacion.application.dto.response.VerifyPhoneOtpResponse::verified);
    }

    private Mono<ProcessValidationInstance> recordValidationInstance(
            Long providerId, String processCode, Map<String, Object> evidence) {

        String entityId = String.valueOf(providerId);
        String alertUuid = UUID.randomUUID().toString();
        Instant now = Instant.now();

        return instanceRepo.findByEntityAndProcessAndValidation(
                        "USER", entityId, processCode, VALIDATION_CODE_OTP)
                .flatMap(existing -> {
                    ProcessValidationInstance updated = new ProcessValidationInstance(
                            existing.id(),
                            existing.entityType(),
                            existing.entityId(),
                            existing.processCode(),
                            existing.validationCode(),
                            ValidationMode.ACTIVE,
                            ValidationStatus.PASSED,
                            evidence,
                            existing.reviewerId(),
                            existing.alertUuid(),
                            existing.createdAt(),
                            now,
                            existing.createdBy(),
                            "SYSTEM"
                    );
                    return instanceRepo.save(updated);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    ProcessValidationInstance newInstance = new ProcessValidationInstance(
                            null,
                            "USER",
                            entityId,
                            processCode,
                            VALIDATION_CODE_OTP,
                            ValidationMode.ACTIVE,
                            ValidationStatus.PASSED,
                            evidence,
                            null,
                            alertUuid,
                            now,
                            now,
                            "SYSTEM",
                            "SYSTEM"
                    );
                    return instanceRepo.save(newInstance);
                }));
    }

    private ProviderBrebKeyResponse toResponse(ProviderBrebKey key) {
        return new ProviderBrebKeyResponse(
                key.id(),
                key.providerId(),
                key.keyType(),
                key.keyValue(),
                key.maskedKeyValue(),
                key.bank(),
                key.phone(),
                key.status(),
                key.createdAt(),
                key.updatedAt()
        );
    }
}
