package com.findu.validacion.infrastructure.adapter.in.web.controller;

import com.findu.validacion.application.dto.request.SupportResolveRequest;
import com.findu.validacion.application.dto.response.SupportResolveResponse;
import com.findu.validacion.application.port.in.SupportResolveUseCase;
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
@RequestMapping("/api/v1/validations/support")
@RequiredArgsConstructor
@Tag(name = "Support", description = "Resolución manual de validaciones backoffice por soporte")
public class SupportController {

    private final SupportResolveUseCase supportResolveUseCase;

    @Operation(summary = "Resolver validación manual por soporte",
            description = "Marca la instancia como PASSED/FAILED y emite ValidationResolvedEvent a RabbitMQ.")
    @PostMapping("/resolve")
    public Mono<ResponseEntity<SupportResolveResponse>> resolve(@Valid @RequestBody SupportResolveRequest request) {
        return supportResolveUseCase.resolve(request)
                .map(ResponseEntity::ok);
    }
}
