package com.findu.notification.processor.infrastructure.adapter.input.web.controller;

import com.findu.notification.processor.application.dto.response.ScreenAlertResponse;
import com.findu.notification.processor.application.port.in.ResolveScreenAlertsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Superficie de lectura de alertas de UI por pantalla (diseño §7.4). Permite al cliente
 * obtener el contenido editable de las plantillas tipo ALERTA de una pantalla sin desplegar
 * la app; el cliente evalúa {@code condicionAparicion} contra el estado del perfil y
 * renderiza las que apliquen, ordenadas por {@code prioridad}.
 */
@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
@Tag(name = "Alertas por pantalla", description = "Lectura del contenido de alertas de UI (motor de plantillas dinámicas)")
public class AlertTemplateController {

    private final ResolveScreenAlertsUseCase resolveScreenAlertsUseCase;

    @GetMapping("/alertas")
    @Operation(summary = "Listar alertas de una pantalla",
            description = "Devuelve las plantillas ALERTA activas de la pantalla (resueltas por código, sin GSI). " +
                    "El cliente evalúa condicionAparicion y ordena por prioridad.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alertas obtenidas exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ScreenAlertResponse.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public Mono<ResponseEntity<List<ScreenAlertResponse>>> listByScreen(
            @Parameter(description = "Pantalla destino de las alertas", required = true, example = "perfil_proveedor")
            @RequestParam String pantalla,
            @Parameter(description = "Idioma de la plantilla (fallback a es)", example = "es")
            @RequestParam(defaultValue = "es") String language) {
        return Mono.fromCallable(() -> resolveScreenAlertsUseCase.resolveForScreen(pantalla, language))
                .map(ResponseEntity::ok);
    }
}
