package com.findu.validacion.infrastructure.adapter.in.web.controller;

import com.findu.validacion.application.dto.response.ActiveAlertDto;
import com.findu.validacion.application.port.in.GetActiveAlertsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/validations/alerts")
@RequiredArgsConstructor
@Tag(name = "Alerts", description = "Consulta PULL de alertas activas por entidad/usuario")
public class ValidationAlertController {

    private final GetActiveAlertsUseCase getActiveAlertsUseCase;

    @Operation(summary = "Obtener alertas activas por usuario o entidad",
            description = "Devuelve las alertas activas (reglas PASSIVE en estado PENDING con alertTemplateCode)")
    @GetMapping
    public Mono<ResponseEntity<List<ActiveAlertDto>>> getAlerts(
            @RequestParam(required = false, defaultValue = "USER") String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) Long userId) {

        String effectiveEntityId = (entityId != null && !entityId.isBlank())
                ? entityId
                : (userId != null ? String.valueOf(userId) : null);

        if (effectiveEntityId == null || effectiveEntityId.isBlank()) {
            return Mono.just(ResponseEntity.ok(List.of()));
        }

        return getActiveAlertsUseCase.getActiveAlerts(entityType, effectiveEntityId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.ok(List.of()));
    }
}
