package com.findu.security.application.port.output.persistence;

import com.findu.security.domain.model.ProviderActivation;
import com.findu.security.domain.model.ValidationState;
import com.findu.security.domain.model.ValidationType;

import java.math.BigDecimal;
import java.time.LocalDate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Puerto de salida para la persistencia del agregado de activación del proveedor.
 * Compone estado desde {@code "user"} y las filas vigentes de {@code provider_activation_validation}.
 */
public interface ProviderActivationRepositoryPort {

    /**
     * Carga el agregado de activación de un usuario (estado + flags + guardas de soporte + foto frontal).
     */
    Mono<ProviderActivation> loadByUserId(Long userId);

    /**
     * Inserta una nueva versión {@code is_current=true} de una validación, dejando la anterior
     * como no vigente. Idempotente respecto al estado final (histórico/versionado).
     *
     * @return el agregado recargado tras la escritura.
     */
    Mono<ProviderActivation> upsertValidation(Long userId, ValidationType tipo, ValidationState estado, String detalle);

    /**
     * Persiste los campos de activación en la fila {@code "user"}: estado, flag de notificación a
     * soporte y código de ticket. No toca autenticación/login.
     */
    Mono<Void> updateUserActivationState(Long userId, String estado, boolean soporteNotified, String soporteTicketCode);

    /**
     * Inserta una captura biométrica y, write-once, fija la foto frontal si aún no hay una.
     * Devuelve la URL de la foto frontal vigente (la ya fijada si existía, o la nueva).
     */
    Mono<String> insertBiometricCapture(Long userId, String cedulaNombre, LocalDate cedulaFechaNac,
                                        BigDecimal faceMatchScore, String fotoFrontalUrl, ValidationState resultado);

    /**
     * Usuarios en {@code EN_REVISION_SOPORTE} cuyo ticket de soporte aún no se creó (reintento).
     */
    Flux<ProviderActivation> findPendingSupportDispatch();
}
