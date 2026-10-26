package com.findu.validacion.application.service;

import com.findu.validacion.application.dto.event.PendingValidationAlertEvent;
import com.findu.validacion.application.dto.event.ValidationResolvedEvent;
import com.findu.validacion.application.dto.request.CheckActiveRequest;
import com.findu.validacion.application.dto.request.CompleteProcessRequest;
import com.findu.validacion.application.dto.request.InspectRequest;
import com.findu.validacion.application.dto.request.SupportResolveRequest;
import com.findu.validacion.application.dto.request.ValidationEvidenceRequest;
import com.findu.validacion.application.dto.response.ActiveValidationDto;
import com.findu.validacion.application.dto.response.CheckActiveResponse;
import com.findu.validacion.application.dto.response.CompleteProcessResponse;
import com.findu.validacion.application.dto.response.FailedValidationDto;
import com.findu.validacion.application.dto.response.InspectResponse;
import com.findu.validacion.application.dto.response.PassiveValidationDto;
import com.findu.validacion.application.dto.response.SupportResolveResponse;
import com.findu.validacion.application.port.in.CheckActiveValidationsUseCase;
import com.findu.validacion.application.port.in.CompleteProcessUseCase;
import com.findu.validacion.application.port.in.InspectEndpointUseCase;
import com.findu.validacion.application.port.in.SupportResolveUseCase;
import com.findu.validacion.application.port.out.AlertEventPublisherPort;
import com.findu.validacion.application.port.out.EndpointValidationConfigRepositoryPort;
import com.findu.validacion.application.port.out.MonitoredEndpointRepositoryPort;
import com.findu.validacion.application.port.out.ProcessValidationInstanceRepositoryPort;
import com.findu.validacion.application.port.out.ValidationDefinitionRepositoryPort;
import com.findu.validacion.domain.enums.HttpMethod;
import com.findu.validacion.domain.enums.ValidationMode;
import com.findu.validacion.domain.enums.ValidationStatus;
import com.findu.validacion.domain.exception.EndpointNotMonitoredException;
import com.findu.validacion.domain.exception.ValidationNotFoundException;
import com.findu.validacion.domain.model.EndpointValidationConfig;
import com.findu.validacion.domain.model.ProcessValidationInstance;
import com.findu.validacion.domain.model.ValidationDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ValidationEngineService implements InspectEndpointUseCase,
        CheckActiveValidationsUseCase,
        CompleteProcessUseCase,
        SupportResolveUseCase {

    private final MonitoredEndpointRepositoryPort endpointRepo;
    private final ValidationDefinitionRepositoryPort defRepo;
    private final EndpointValidationConfigRepositoryPort configRepo;
    private final ProcessValidationInstanceRepositoryPort instanceRepo;
    private final AlertEventPublisherPort alertPublisher;

    @Override
    public Mono<InspectResponse> inspect(InspectRequest request) {
        log.info("Inspecting endpoint serviceName={}, method={}, path={}",
                request.serviceName(), request.httpMethod(), request.path());

        HttpMethod method;
        try {
            method = HttpMethod.valueOf(request.httpMethod().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Mono.error(new IllegalArgumentException("Invalid HTTP method: " + request.httpMethod()));
        }

        return endpointRepo.findByServiceAndMethodAndPath(request.serviceName(), method, request.path())
                .switchIfEmpty(Mono.error(new EndpointNotMonitoredException(
                        "Endpoint not monitored: " + request.serviceName() + " " + request.httpMethod() + " " + request.path())))
                .flatMap(endpoint -> configRepo.findByProcessCode(endpoint.processCode())
                        .switchIfEmpty(Mono.just(new EndpointValidationConfig(null, endpoint.processCode(), List.of())))
                        .flatMap(config -> {
                            List<EndpointValidationConfig.ConfigItem> validations =
                                    config.validations() != null ? config.validations() : List.of();
                            List<String> codes = validations.stream()
                                    .map(EndpointValidationConfig.ConfigItem::validationCode)
                                    .toList();

                            return defRepo.findByValidationCodeIn(codes)
                                    .collectMap(ValidationDefinition::validationCode, d -> d)
                                    .map(defsMap -> {
                                        List<ActiveValidationDto> activeList = new ArrayList<>();
                                        List<PassiveValidationDto> passiveList = new ArrayList<>();

                                        for (EndpointValidationConfig.ConfigItem item : validations) {
                                            ValidationDefinition def = defsMap.get(item.validationCode());
                                            if (item.mode() == ValidationMode.ACTIVE) {
                                                activeList.add(new ActiveValidationDto(
                                                        item.validationCode(),
                                                        item.mandatory(),
                                                        def != null ? def.resolutionEndpoint() : null
                                                ));
                                            } else {
                                                passiveList.add(new PassiveValidationDto(
                                                        item.validationCode(),
                                                        def != null ? def.alertTemplateCode() : null,
                                                        def != null ? def.executionType() : null,
                                                        def != null ? def.resolutionEndpoint() : null
                                                ));
                                            }
                                        }

                                        return new InspectResponse(endpoint.processCode(), activeList, passiveList);
                                    });
                        }));
    }

    @Override
    public Mono<CheckActiveResponse> check(CheckActiveRequest request) {
        log.info("Checking active validations for processCode={}, entityId={}",
                request.processCode(), request.entityId());

        return configRepo.findByProcessCode(request.processCode())
                .flatMap(config -> {
                    List<EndpointValidationConfig.ConfigItem> activeConfigs = config.validations() != null
                            ? config.validations().stream()
                            .filter(c -> c.mode() == ValidationMode.ACTIVE)
                            .toList()
                            : List.of();

                    if (activeConfigs.isEmpty()) {
                        return Mono.just(new CheckActiveResponse(true, List.of()));
                    }

                    Map<String, Map<String, Object>> providedEvidences = request.evidences() != null
                            ? request.evidences().stream()
                            .filter(e -> e.validationCode() != null && e.payload() != null && !e.payload().isEmpty())
                            .collect(Collectors.toMap(
                                    ValidationEvidenceRequest::validationCode,
                                    ValidationEvidenceRequest::payload,
                                    (a, b) -> a))
                            : Collections.emptyMap();

                    List<FailedValidationDto> failed = new ArrayList<>();
                    for (EndpointValidationConfig.ConfigItem item : activeConfigs) {
                        if (item.mandatory() && !providedEvidences.containsKey(item.validationCode())) {
                            String errorKey = item.errorMessageKey() != null
                                    ? item.errorMessageKey()
                                    : item.validationCode() + ".required";
                            failed.add(new FailedValidationDto(item.validationCode(), errorKey));
                        }
                    }

                    if (!failed.isEmpty()) {
                        return Mono.just(new CheckActiveResponse(false, failed));
                    }
                    return Mono.just(new CheckActiveResponse(true, List.of()));
                })
                .defaultIfEmpty(new CheckActiveResponse(true, List.of())); // Fail-open: not configured = allowed
    }

    @Override
    public Mono<CompleteProcessResponse> complete(CompleteProcessRequest request) {
        log.info("Completing process for processCode={}, entityId={}, userId={}",
                request.processCode(), request.entityId(), request.userId());

        String entityType = request.entityType() != null && !request.entityType().isBlank()
                ? request.entityType()
                : "USER";
        Long userId = request.userId() != null
                ? request.userId()
                : parseUserId(request.entityId());
        List<String> satisfied = request.satisfied() != null
                ? request.satisfied()
                : List.of();

        return configRepo.findByProcessCode(request.processCode())
                .flatMap(config -> {
                    List<EndpointValidationConfig.ConfigItem> passivePending = config.validations() != null
                            ? config.validations().stream()
                            .filter(c -> c.mode() == ValidationMode.PASSIVE)
                            .filter(c -> !satisfied.contains(c.validationCode()))
                            .toList()
                            : List.of();

                    if (passivePending.isEmpty()) {
                        return Mono.just(new CompleteProcessResponse(request.processCode(), List.of(), 0));
                    }

                    return Flux.fromIterable(passivePending)
                            .concatMap(c -> {
                                String alertUuid = UUID.randomUUID().toString();
                                Map<String, Object> evidenceMap = extractEvidence(request.evidences(), c.validationCode());

                                return instanceRepo.findByEntityAndProcessAndValidation(
                                                entityType, request.entityId(), request.processCode(), c.validationCode())
                                        .flatMap(existing -> {
                                            // Update existing pending instance with new evidence and alertUuid
                                            ProcessValidationInstance updated = new ProcessValidationInstance(
                                                    existing.id(),
                                                    existing.entityType(),
                                                    existing.entityId(),
                                                    existing.processCode(),
                                                    existing.validationCode(),
                                                    ValidationMode.PASSIVE,
                                                    ValidationStatus.PENDING,
                                                    !evidenceMap.isEmpty() ? evidenceMap : existing.evidence(),
                                                    existing.reviewerId(),
                                                    alertUuid,
                                                    existing.createdAt(),
                                                    Instant.now(),
                                                    existing.createdBy(),
                                                    "SYSTEM"
                                            );
                                            return instanceRepo.save(updated);
                                        })
                                        .switchIfEmpty(Mono.defer(() -> {
                                            ProcessValidationInstance newInstance = new ProcessValidationInstance(
                                                    null,
                                                    entityType,
                                                    request.entityId(),
                                                    request.processCode(),
                                                    c.validationCode(),
                                                    ValidationMode.PASSIVE,
                                                    ValidationStatus.PENDING,
                                                    evidenceMap,
                                                    null,
                                                    alertUuid,
                                                    Instant.now(),
                                                    Instant.now(),
                                                    "SYSTEM",
                                                    "SYSTEM"
                                            );
                                            return instanceRepo.save(newInstance);
                                        }))
                                        .flatMap(savedInstance ->
                                                defRepo.findByValidationCode(c.validationCode())
                                                        .defaultIfEmpty(new ValidationDefinition(
                                                                null, c.validationCode(), null, null, null, null, null, null))
                                                        .flatMap(def -> {
                                                            PendingValidationAlertEvent event = new PendingValidationAlertEvent(
                                                                    UUID.randomUUID().toString(),
                                                                    userId,
                                                                    request.processCode(),
                                                                    c.validationCode(),
                                                                    def.alertTemplateCode(),
                                                                    "PROVEEDOR_" + userId,
                                                                    alertUuid
                                                            );
                                                            return alertPublisher.publishPending(event)
                                                                    .onErrorResume(e -> {
                                                                        log.warn("Error publicando evento de alerta pendiente para validationCode={}: {}",
                                                                                c.validationCode(), e.getMessage());
                                                                        return Mono.empty();
                                                                    });
                                                        })
                                                        .thenReturn(c.validationCode())
                                        );
                            })
                            .collectList()
                            .map(pendingList -> new CompleteProcessResponse(
                                    request.processCode(), pendingList, pendingList.size()));
                })
                .defaultIfEmpty(new CompleteProcessResponse(request.processCode(), List.of(), 0)); // Fail-open
    }

    @Override
    public Mono<SupportResolveResponse> resolve(SupportResolveRequest request) {
        log.info("Resolving validation support for processCode={}, entityId={}, validationCode={}, resolution={}",
                request.processCode(), request.entityId(), request.validationCode(), request.resolution());

        String entityType = request.entityType() != null && !request.entityType().isBlank()
                ? request.entityType()
                : "USER";

        ValidationStatus resolutionStatus;
        try {
            resolutionStatus = ValidationStatus.valueOf(request.resolution().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Mono.error(new IllegalArgumentException("Invalid resolution status: " + request.resolution()));
        }

        return instanceRepo.findByEntityAndProcessAndValidation(
                        entityType, request.entityId(), request.processCode(), request.validationCode())
                .switchIfEmpty(Mono.error(new ValidationNotFoundException(
                        "Validation instance not found for entity " + request.entityId()
                                + ", process " + request.processCode()
                                + ", validation " + request.validationCode())))
                .flatMap(existing -> {
                    ProcessValidationInstance resolved = existing.resolve(
                            resolutionStatus, request.reviewerId(), request.nota());

                    Long userId = parseUserId(request.entityId());

                    return instanceRepo.save(resolved)
                            .flatMap(saved -> {
                                ValidationResolvedEvent event = new ValidationResolvedEvent(
                                        UUID.randomUUID().toString(),
                                        userId,
                                        request.validationCode(),
                                        saved.alertUuid(),
                                        request.resolution()
                                );
                                return alertPublisher.publishResolved(event).thenReturn(saved);
                            })
                            .map(saved -> new SupportResolveResponse(saved.status().name(), saved.alertUuid()));
                });
    }

    private Long parseUserId(String entityId) {
        if (entityId == null || entityId.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(entityId);
        } catch (NumberFormatException e) {
            log.warn("Could not parse entityId '{}' as Long userId", entityId);
            return null;
        }
    }

    private Map<String, Object> extractEvidence(List<ValidationEvidenceRequest> evidences, String validationCode) {
        if (evidences == null || validationCode == null) {
            return Collections.emptyMap();
        }
        return evidences.stream()
                .filter(e -> validationCode.equals(e.validationCode()) && e.payload() != null)
                .findFirst()
                .map(ValidationEvidenceRequest::payload)
                .orElse(Collections.emptyMap());
    }
}
