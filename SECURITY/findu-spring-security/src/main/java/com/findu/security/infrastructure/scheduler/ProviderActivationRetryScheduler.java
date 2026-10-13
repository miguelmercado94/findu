package com.findu.security.infrastructure.scheduler;

import com.findu.security.application.port.output.externalapi.CorePerfilPort;
import com.findu.security.application.port.output.persistence.ProviderActivationRepositoryPort;
import com.findu.security.application.usecase.ActivationProgressService;
import com.findu.security.domain.model.ProviderActivation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Proceso aislado (migrable a Lambda): reintenta la creación del ticket de soporte para proveedores
 * en {@code EN_REVISION_SOPORTE} con {@code soporte_notified=false}, y re-propaga a core la foto
 * frontal de esos proveedores (best-effort). La re-creación del ticket es idempotente (guarda
 * {@code soporte_notified} + dedup por externalRef en help-v2); el re-push a core está protegido por
 * la regla write-once del lado de core.
 */
@Component
@ConditionalOnProperty(name = "findu.activation.retry-enabled", havingValue = "true", matchIfMissing = true)
public class ProviderActivationRetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(ProviderActivationRetryScheduler.class);

    private final ProviderActivationRepositoryPort activationRepository;
    private final ActivationProgressService activationProgressService;
    private final CorePerfilPort corePerfilPort;

    public ProviderActivationRetryScheduler(ProviderActivationRepositoryPort activationRepository,
                                            ActivationProgressService activationProgressService,
                                            CorePerfilPort corePerfilPort) {
        this.activationRepository = activationRepository;
        this.activationProgressService = activationProgressService;
        this.corePerfilPort = corePerfilPort;
    }

    @Scheduled(fixedDelayString = "${findu.activation.retry-interval-ms:900000}",
            initialDelayString = "${findu.activation.retry-interval-ms:900000}")
    public void retryPending() {
        activationRepository.findPendingSupportDispatch()
                .flatMap(this::retryOne)
                .subscribe(
                        a -> {},
                        e -> log.warn("Reintento de activación falló: {}", e.toString()),
                        () -> log.debug("Reintento de activación: barrido completado"));
    }

    private Mono<ProviderActivation> retryOne(ProviderActivation activation) {
        Long userId = activation.getUserId();
        Mono<Void> photoSync = Mono.empty();
        if (activation.getFotoFrontalUrl() != null && !activation.getFotoFrontalUrl().isBlank()) {
            photoSync = corePerfilPort.fetchByAuthUserId(userId)
                    .filter(p -> p.urlImagenPerfil() == null || p.urlImagenPerfil().isBlank())
                    .flatMap(p -> corePerfilPort.pushProfilePhoto(p.id(), p.nombreCompleto(), activation.getFotoFrontalUrl()))
                    .onErrorResume(e -> Mono.empty())
                    .then();
        }
        return photoSync.then(activationProgressService.recompute(userId))
                .onErrorResume(e -> {
                    log.warn("Reintento de ticket de soporte falló (userId={}): {}", userId, e.getMessage());
                    return Mono.just(activation);
                });
    }
}
