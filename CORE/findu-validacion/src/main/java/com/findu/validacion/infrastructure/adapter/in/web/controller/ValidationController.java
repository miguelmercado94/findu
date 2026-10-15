package com.findu.validacion.infrastructure.adapter.in.web.controller;

import com.findu.validacion.application.dto.request.CheckActiveRequest;
import com.findu.validacion.application.dto.request.CompleteProcessRequest;
import com.findu.validacion.application.dto.request.InspectRequest;
import com.findu.validacion.application.dto.response.CheckActiveResponse;
import com.findu.validacion.application.dto.response.CompleteProcessResponse;
import com.findu.validacion.application.dto.response.InspectResponse;
import com.findu.validacion.application.port.in.CheckActiveValidationsUseCase;
import com.findu.validacion.application.port.in.CompleteProcessUseCase;
import com.findu.validacion.application.port.in.InspectEndpointUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1/validations")
@RequiredArgsConstructor
@Tag(name = "Validations", description = "Motor de validaciones declarativo y orquestación de procesos")
public class ValidationController {

    private final InspectEndpointUseCase inspectUseCase;
    private final CheckActiveValidationsUseCase checkActiveUseCase;
    private final CompleteProcessUseCase completeProcessUseCase;

    @Operation(summary = "Inspeccionar reglas de validación de un endpoint",
            description = "Devuelve las validaciones ACTIVE y PASSIVE configuradas para el endpoint. 404 si no está monitoreado.")
    @PostMapping("/inspect")
    public Mono<ResponseEntity<InspectResponse>> inspect(@Valid @RequestBody InspectRequest request) {
        return inspectUseCase.inspect(request)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Validar reglas activas/bloqueantes",
            description = "Evalúa si las validaciones ACTIVE obligatorias están satisfechas antes de persistir mutación de negocio.")
    @PostMapping("/check-active")
    public Mono<ResponseEntity<CheckActiveResponse>> checkActive(@Valid @RequestBody CheckActiveRequest request) {
        return checkActiveUseCase.check(request)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Completar proceso de negocio",
            description = "Registra las validaciones PASSIVE pendientes como instancias PENDING y emite eventos de alerta a RabbitMQ.")
    @PostMapping("/complete-process")
    public Mono<ResponseEntity<CompleteProcessResponse>> completeProcess(@Valid @RequestBody CompleteProcessRequest request) {
        return completeProcessUseCase.complete(request)
                .map(ResponseEntity::ok);
    }
}
