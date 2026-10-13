package com.findu.security.infrastructure.adapter.persistence;

import com.findu.security.application.port.output.persistence.ProviderActivationRepositoryPort;
import com.findu.security.domain.model.ProviderActivation;
import com.findu.security.domain.model.ProviderActivationState;
import com.findu.security.domain.model.ValidationState;
import com.findu.security.domain.model.ValidationType;
import com.findu.security.infrastructure.entity.ProviderActivationValidationEntity;
import com.findu.security.infrastructure.entity.ProviderBiometricCaptureEntity;
import com.findu.security.infrastructure.entity.UserEntity;
import com.findu.security.infrastructure.repository.ProviderActivationValidationRepository;
import com.findu.security.infrastructure.repository.ProviderBiometricCaptureRepository;
import com.findu.security.infrastructure.repository.UserRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Adapter de persistencia del agregado de activación. Compone el estado desde la fila {@code "user"}
 * (estado + guardas de soporte + foto frontal vigente) y las filas vigentes de
 * {@code provider_activation_validation}. Las escrituras de validación son versionadas (histórico).
 */
@Component
public class ProviderActivationRepoAdapter implements ProviderActivationRepositoryPort {

    private static final String AUDIT_USER = "system";

    private final UserRepository userRepository;
    private final ProviderActivationValidationRepository validationRepository;
    private final ProviderBiometricCaptureRepository biometricRepository;

    public ProviderActivationRepoAdapter(UserRepository userRepository,
                                         ProviderActivationValidationRepository validationRepository,
                                         ProviderBiometricCaptureRepository biometricRepository) {
        this.userRepository = userRepository;
        this.validationRepository = validationRepository;
        this.biometricRepository = biometricRepository;
    }

    @Override
    public Mono<ProviderActivation> loadByUserId(Long userId) {
        return userRepository.findById(userId)
                .flatMap(this::composeFromUser);
    }

    private Mono<ProviderActivation> composeFromUser(UserEntity user) {
        ProviderActivation activation = new ProviderActivation();
        activation.setUserId(user.getId());
        activation.setEstado(parseEstado(user.getEstadoActivacion()));
        activation.setSoporteNotified(user.isSoporteNotified());
        activation.setSoporteTicketCode(user.getSoporteTicketCode());
        // Flags: OTP y SOPORTE desde las filas vigentes; EMAIL desde email_verified (fuente canónica).
        activation.setEmailOk(user.isEmailVerified());
        return validationRepository.findCurrentByUser(user.getId())
                .doOnNext(v -> applyFlag(activation, v))
                .then(biometricRepository.findLatestOkWithPhoto(user.getId())
                        .doOnNext(cap -> activation.setFotoFrontalUrl(cap.getFotoFrontalUrl()))
                        .then())
                .thenReturn(activation);
    }

    private void applyFlag(ProviderActivation activation, ProviderActivationValidationEntity v) {
        boolean ok = ValidationState.OK.name().equals(v.getEstado());
        switch (ValidationType.valueOf(v.getTipo())) {
            case OTP -> activation.setOtpOk(ok);
            case EMAIL -> {
                if (ok) {
                    activation.setEmailOk(true);
                }
            }
            case BIOMETRIA -> activation.setBiometriaOk(ok);
            case SOPORTE -> activation.setSoporteOk(ok);
        }
    }

    private ProviderActivationState parseEstado(String raw) {
        if (raw == null || raw.isBlank()) {
            return ProviderActivationState.PENDIENTE_VALIDACIONES;
        }
        return ProviderActivationState.valueOf(raw);
    }

    @Override
    public Mono<ProviderActivation> upsertValidation(Long userId, ValidationType tipo, ValidationState estado, String detalle) {
        return validationRepository.findCurrentByUserAndTipo(userId, tipo.name())
                .map(existing -> existing.getVersion() == null ? 1 : existing.getVersion() + 1)
                .defaultIfEmpty(1)
                .flatMap(nextVersion -> validationRepository.markPreviousNotCurrent(userId, tipo.name())
                        .then(Mono.defer(() -> {
                            LocalDateTime now = LocalDateTime.now();
                            boolean completed = estado == ValidationState.OK || estado == ValidationState.RECHAZADA;
                            ProviderActivationValidationEntity entity = ProviderActivationValidationEntity.builder()
                                    .userId(userId)
                                    .tipo(tipo.name())
                                    .estado(estado.name())
                                    .version(nextVersion)
                                    .isCurrent(true)
                                    .detalle(detalle)
                                    .completedAt(completed ? now : null)
                                    .createdAt(now)
                                    .updatedAt(now)
                                    .createdBy(AUDIT_USER)
                                    .updatedBy(AUDIT_USER)
                                    .build();
                            return validationRepository.save(entity);
                        })))
                .then(loadByUserId(userId));
    }

    @Override
    public Mono<Void> updateUserActivationState(Long userId, String estado, boolean soporteNotified, String soporteTicketCode) {
        return userRepository.findById(userId)
                .flatMap(user -> {
                    user.setEstadoActivacion(estado);
                    user.setSoporteNotified(soporteNotified);
                    user.setSoporteTicketCode(soporteTicketCode);
                    user.setUpdatedAt(LocalDateTime.now());
                    user.setUpdatedBy(user.getUpdatedBy() != null ? user.getUpdatedBy() : AUDIT_USER);
                    return userRepository.save(user);
                })
                .then();
    }

    @Override
    public Mono<String> insertBiometricCapture(Long userId, String cedulaNombre, LocalDate cedulaFechaNac,
                                               BigDecimal faceMatchScore, String fotoFrontalUrl, ValidationState resultado) {
        return biometricRepository.findLatestOkWithPhoto(userId)
                .map(ProviderBiometricCaptureEntity::getFotoFrontalUrl)
                .defaultIfEmpty("")
                .flatMap(existingPhoto -> {
                    // write-once: si ya hay una foto frontal fijada, se conserva y no se re-persiste la nueva.
                    boolean hasExisting = existingPhoto != null && !existingPhoto.isBlank();
                    String photoToStore = hasExisting ? null : fotoFrontalUrl;
                    ProviderBiometricCaptureEntity entity = ProviderBiometricCaptureEntity.builder()
                            .userId(userId)
                            .cedulaNombre(cedulaNombre)
                            .cedulaFechaNac(cedulaFechaNac)
                            .faceMatchScore(faceMatchScore)
                            .fotoFrontalUrl(photoToStore)
                            .resultado(resultado.name())
                            .createdAt(LocalDateTime.now())
                            .build();
                    return biometricRepository.save(entity)
                            .thenReturn(hasExisting ? existingPhoto : fotoFrontalUrl);
                });
    }

    @Override
    public Flux<ProviderActivation> findPendingSupportDispatch() {
        return userRepository.findPendingSupportDispatch()
                .flatMap(this::composeFromUser);
    }
}
