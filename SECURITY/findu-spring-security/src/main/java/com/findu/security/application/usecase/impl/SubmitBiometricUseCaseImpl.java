package com.findu.security.application.usecase.impl;

import com.findu.security.application.port.output.externalapi.CorePerfilPort;
import com.findu.security.application.port.output.externalapi.ValidacionResolvePort;
import com.findu.security.application.port.output.persistence.ProviderActivationRepositoryPort;
import com.findu.security.application.service.AuthenticatedUserResolver;
import com.findu.security.application.usecase.ActivationProgressService;
import com.findu.security.application.usecase.SubmitBiometricUseCase;
import com.findu.security.domain.model.Usuario;
import com.findu.security.domain.model.ValidationState;
import com.findu.security.domain.model.ValidationType;
import com.findu.security.dto.request.BiometricSubmitRequest;
import com.findu.security.dto.response.ActivationStatusResponse;
import com.findu.security.util.TextNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Envío biométrico del proveedor. Re-valida en backend nombre (sin tildes) y fecha de nacimiento
 * contra findu-core; confía el face match del cliente (TFLite, mock en dev). Rechazos de negocio ->
 * 200 {biometriaOk:false, motivo} con fila RECHAZADA. Entrada malformada -> 400. En éxito: fija
 * write-once la foto frontal, marca biometria_ok, propaga la foto a core (best-effort) y recomputa.
 */
@Service
public class SubmitBiometricUseCaseImpl implements SubmitBiometricUseCase {

    private static final Logger log = LoggerFactory.getLogger(SubmitBiometricUseCaseImpl.class);

    private static final String MOTIVO_NOMBRE = "nombre_no_coincide";
    private static final String MOTIVO_FECHA = "fecha_no_coincide";
    private static final String MOTIVO_ROSTRO = "rostro_no_coincide";
    private static final String VALIDATION_CODE_BIOMETRIC = "BIOMETRIC_FACIAL";
    private static final String NOTA_BIOMETRIC_PASSED = "auto:biometric-passed";

    private final AuthenticatedUserResolver authenticatedUserResolver;
    private final CorePerfilPort corePerfilPort;
    private final ProviderActivationRepositoryPort activationRepository;
    private final ActivationProgressService activationProgressService;
    private final ValidacionResolvePort validacionResolvePort;

    @Value("${findu.activation.biometric.mock-always-pass:true}")
    private boolean mockAlwaysPass;

    public SubmitBiometricUseCaseImpl(AuthenticatedUserResolver authenticatedUserResolver,
                                      CorePerfilPort corePerfilPort,
                                      ProviderActivationRepositoryPort activationRepository,
                                      ActivationProgressService activationProgressService,
                                      ValidacionResolvePort validacionResolvePort) {
        this.authenticatedUserResolver = authenticatedUserResolver;
        this.corePerfilPort = corePerfilPort;
        this.activationRepository = activationRepository;
        this.activationProgressService = activationProgressService;
        this.validacionResolvePort = validacionResolvePort;
    }

    @Override
    public Mono<ActivationStatusResponse> submit(BiometricSubmitRequest request) {
        // Validación de forma: fecha ISO parseable y no futura (malformado -> 400).
        LocalDate fechaRequest;
        try {
            fechaRequest = LocalDate.parse(request.cedulaFechaNacimiento().trim());
        } catch (DateTimeParseException e) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "fecha_malformada"));
        }
        if (fechaRequest.isAfter(LocalDate.now())) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "fecha_futura"));
        }

        return authenticatedUserResolver.resolve()
                .flatMap(user -> corePerfilPort.fetchByAuthUserId(user.getId())
                        .defaultIfEmpty(new CorePerfilPort.PerfilProveedor(user.getId(), request.cedulaNombre(), fechaRequest, null))
                        .flatMap(perfil -> evaluate(user, perfil, request, fechaRequest)));
    }

    private Mono<ActivationStatusResponse> evaluate(Usuario user, CorePerfilPort.PerfilProveedor perfil,
                                                    BiometricSubmitRequest request, LocalDate fechaRequest) {
        Long userId = user.getId();

        // En modo mock / dev (mockAlwaysPass = true), se aprueba siempre que se reciban los datos biométricos.
        if (!mockAlwaysPass) {
            // Rechazos de negocio (200 con motivo), en orden: nombre, fecha, rostro.
            if (!TextNormalizer.equalsIgnoringAccents(request.cedulaNombre(), perfil.nombreCompleto())) {
                return businessRejection(userId, MOTIVO_NOMBRE);
            }
            if (perfil.fechaNacimiento() == null || !perfil.fechaNacimiento().isEqual(fechaRequest)) {
                return businessRejection(userId, MOTIVO_FECHA);
            }
            if (!Boolean.TRUE.equals(request.faceMatched())) {
                return businessRejection(userId, MOTIVO_ROSTRO);
            }
        } else {
            log.info("Biometria mock mode activo: validando exitosamente para userId={}", userId);
        }

        String nombreFinal = (perfil.nombreCompleto() != null && !perfil.nombreCompleto().isBlank())
                ? perfil.nombreCompleto()
                : request.cedulaNombre();
        LocalDate fechaFinal = perfil.fechaNacimiento() != null ? perfil.fechaNacimiento() : fechaRequest;

        // Éxito: captura OK (write-once foto), marca biometria_ok, propaga foto best-effort, recomputa y avisa a findu-validacion.
        return activationRepository.insertBiometricCapture(userId, nombreFinal, fechaFinal,
                        request.faceMatchScore(), request.fotoFrontalUrl(), ValidationState.OK)
                .flatMap(fotoVigente -> activationRepository
                        .upsertValidation(userId, ValidationType.BIOMETRIA, ValidationState.OK, null)
                        .then(corePerfilPort.pushProfilePhoto(perfil.id(), nombreFinal, fotoVigente)
                                .onErrorResume(e -> {
                                    log.warn("Propagación de foto a core falló (userId={}, best-effort): {}",
                                            userId, e.getMessage());
                                    return Mono.empty();
                                }))
                        .then(activationProgressService.recompute(userId)))
                .flatMap(activation -> validacionResolvePort.markPassed(userId, VALIDATION_CODE_BIOMETRIC, NOTA_BIOMETRIC_PASSED)
                        .onErrorResume(e -> {
                            log.warn("Resolución de validación biométrica en findu-validacion falló (userId={}, best-effort): {}",
                                    userId, e.getMessage());
                            return Mono.empty();
                        })
                        .thenReturn(activation))
                .map(ActivationStatusResponse::from);
    }

    private Mono<ActivationStatusResponse> businessRejection(Long userId, String motivo) {
        return activationRepository.upsertValidation(userId, ValidationType.BIOMETRIA, ValidationState.RECHAZADA, motivo)
                .map(activation -> ActivationStatusResponse.rejected(activation, motivo));
    }
}
