package com.findu.validacion.application.service;

import com.findu.validacion.application.dto.response.ActiveAlertDto;
import com.findu.validacion.application.port.in.GetActiveAlertsUseCase;
import com.findu.validacion.application.port.out.EndpointValidationConfigRepositoryPort;
import com.findu.validacion.application.port.out.ProcessValidationInstanceRepositoryPort;
import com.findu.validacion.application.port.out.ValidationDefinitionRepositoryPort;
import com.findu.validacion.domain.enums.ValidationMode;
import com.findu.validacion.domain.enums.ValidationStatus;
import com.findu.validacion.domain.model.EndpointValidationConfig;
import com.findu.validacion.domain.model.ProcessValidationInstance;
import com.findu.validacion.domain.model.ValidationDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetActiveAlertsService implements GetActiveAlertsUseCase {

    private final ProcessValidationInstanceRepositoryPort instanceRepo;
    private final EndpointValidationConfigRepositoryPort configRepo;
    private final ValidationDefinitionRepositoryPort defRepo;

    @Override
    public Mono<List<ActiveAlertDto>> getActiveAlerts(String entityType, String entityId) {
        log.info("Getting active alerts for entityType={}, entityId={}", entityType, entityId);

        String effectiveEntityType = (entityType != null && !entityType.isBlank()) ? entityType : "USER";

        return instanceRepo.findByEntityAndStatus(effectiveEntityType, entityId, ValidationStatus.PENDING)
                .flatMap(instance -> {
                    // Verificar si la regla es PASSIVE
                    Mono<Boolean> isPassiveMono;
                    if (instance.processCode() != null && !instance.processCode().isBlank()) {
                        isPassiveMono = configRepo.findByProcessCode(instance.processCode())
                                .map(config -> {
                                    if (config.validations() == null) {
                                        return instance.mode() == ValidationMode.PASSIVE;
                                    }
                                    return config.validations().stream()
                                            .filter(c -> instance.validationCode().equals(c.validationCode()))
                                            .findFirst()
                                            .map(c -> c.mode() == ValidationMode.PASSIVE)
                                            .orElse(instance.mode() == ValidationMode.PASSIVE);
                                })
                                .defaultIfEmpty(instance.mode() == ValidationMode.PASSIVE);
                    } else {
                        isPassiveMono = Mono.just(instance.mode() == ValidationMode.PASSIVE);
                    }

                    return isPassiveMono.flatMap(isPassive -> {
                        if (!Boolean.TRUE.equals(isPassive)) {
                            return Mono.empty();
                        }

                        // Verificar si tiene alertTemplateCode en validation_definition
                        return defRepo.findByValidationCode(instance.validationCode())
                                .flatMap(def -> {
                                    if (def.alertTemplateCode() == null || def.alertTemplateCode().isBlank()) {
                                        return Mono.empty();
                                    }
                                    return Mono.just(buildActiveAlert(instance, def));
                                });
                    });
                })
                .collectList()
                .map(alerts -> {
                    alerts.sort(Comparator.comparingInt(ActiveAlertDto::prioridad));
                    return alerts;
                })
                .defaultIfEmpty(List.of());
    }

    private ActiveAlertDto buildActiveAlert(ProcessValidationInstance instance, ValidationDefinition def) {
        String templateCode = def.alertTemplateCode();
        String codigo = (templateCode != null && !templateCode.isBlank()) ? templateCode : instance.validationCode();

        String titulo;
        String body;
        String ctaLabel = null;
        String ctaAction = null;
        String categoria = "INFO";
        int prioridad = 10;
        boolean cerrable = true;
        String pantallaDestino = "home";

        if ("PERFIL_CORREO_PENDIENTE".equals(templateCode) || "EMAIL_VERIFICATION".equals(instance.validationCode())) {
            titulo = "Confirma tu correo";
            body = "Confirma tu correo para avanzar en la activación de tu cuenta.";
            categoria = "INFO";
            prioridad = 1;
            ctaLabel = null;
            ctaAction = null;
            cerrable = true;
            pantallaDestino = "home";
        } else if ("PERFIL_BIOMETRIA_PENDIENTE".equals(templateCode) || "BIOMETRIC_FACIAL".equals(instance.validationCode())) {
            titulo = "Completa tu validación biométrica";
            body = "Completa tu validación biométrica para activar tu cuenta.";
            categoria = "INFO";
            prioridad = 2;
            ctaLabel = "Validar ahora";
            ctaAction = "findu://perfil/verificacion-biometrica";
            cerrable = true;
            pantallaDestino = "home";
        } else {
            titulo = (def.name() != null && !def.name().isBlank()) ? def.name() : "Alerta de Validación";
            body = def.description() != null ? def.description() : "";
            ctaLabel = def.resolutionEndpoint() != null ? "Validar" : null;
            ctaAction = def.resolutionEndpoint();
            categoria = "INFO";
            prioridad = 99;
            cerrable = true;
            pantallaDestino = "home";
        }

        return new ActiveAlertDto(
                instance.alertUuid(),
                codigo,
                instance.validationCode(),
                templateCode,
                true,
                categoria,
                titulo,
                body,
                ctaLabel,
                ctaAction,
                cerrable,
                prioridad,
                pantallaDestino
        );
    }
}
