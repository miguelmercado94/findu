package com.findu.notification.processor.infrastructure.adapter.input.web.controller;

import com.findu.notification.processor.application.dto.request.ActivateVersionRequest;
import com.findu.notification.processor.application.dto.request.CreateTemplateRequest;
import com.findu.notification.processor.application.dto.request.UpdateTemplateRequest;
import com.findu.notification.processor.application.dto.response.TemplateResponse;
import com.findu.notification.processor.application.dto.response.TemplateVersionResponse;
import com.findu.notification.processor.application.port.in.ManageTemplateUseCase;
import com.findu.notification.processor.domain.enums.TemplateType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Superficie de administración de plantillas de notificación (diseño §10). Permite a un
 * rol admin crear, editar (nueva versión), activar/rollback, listar por tipo, consultar el
 * histórico y desactivar plantillas sin desplegar código.
 */
@RestController
@RequestMapping("/api/v1/admin/templates")
@RequiredArgsConstructor
@Tag(name = "Admin Plantillas", description = "Gestión de plantillas de notificación (motor de plantillas dinámicas)")
public class TemplateAdminController {

    private final ManageTemplateUseCase manageTemplateUseCase;

    @PostMapping
    @Operation(summary = "Crear plantilla", description = "Crea una plantilla nueva (versión 1) y la deja activa en una sola transacción")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Plantilla creada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TemplateResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de plantilla inválidos", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public Mono<ResponseEntity<TemplateResponse>> create(@Valid @RequestBody CreateTemplateRequest request) {
        return Mono.fromCallable(() -> manageTemplateUseCase.create(request))
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @PutMapping("/{codigo}/{tipo}/{language}")
    @Operation(summary = "Editar plantilla", description = "Edita una plantilla generando una versión nueva inmutable y repuntando la versión activa")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Nueva versión creada y activada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TemplateResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de plantilla inválidos", content = @Content),
            @ApiResponse(responseCode = "404", description = "Plantilla no encontrada", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public Mono<ResponseEntity<TemplateResponse>> edit(
            @Parameter(description = "Código funcional de la plantilla", required = true, example = "VERIFICACION_OTP")
            @PathVariable String codigo,
            @Parameter(description = "Tipo funcional de la plantilla", required = true, example = "SMS")
            @PathVariable TemplateType tipo,
            @Parameter(description = "Idioma de la plantilla", required = true, example = "es")
            @PathVariable String language,
            @Valid @RequestBody UpdateTemplateRequest request) {
        return Mono.fromCallable(() -> manageTemplateUseCase.edit(codigo, tipo, language, request))
                .map(ResponseEntity::ok);
    }

    @PostMapping("/activate")
    @Operation(summary = "Activar / rollback de versión", description = "Repunta la versión activa a una versión previa concreta (rollback)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Versión activada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TemplateResponse.class))),
            @ApiResponse(responseCode = "400", description = "Petición inválida o versión inexistente", content = @Content),
            @ApiResponse(responseCode = "404", description = "Plantilla no encontrada", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public Mono<ResponseEntity<TemplateResponse>> activateVersion(@Valid @RequestBody ActivateVersionRequest request) {
        return Mono.fromCallable(() -> manageTemplateUseCase.activateVersion(request))
                .map(ResponseEntity::ok);
    }

    @GetMapping
    @Operation(summary = "Listar plantillas por tipo", description = "Lista la versión activa de las plantillas de un tipo funcional")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de plantillas obtenida exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TemplateResponse.class))),
            @ApiResponse(responseCode = "400", description = "Tipo inválido", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public Mono<ResponseEntity<List<TemplateResponse>>> listByType(
            @Parameter(description = "Tipo funcional de plantilla", required = true, example = "SMS")
            @RequestParam TemplateType tipo) {
        return Mono.fromCallable(() -> manageTemplateUseCase.listByType(tipo))
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{codigo}/{tipo}/{language}/history")
    @Operation(summary = "Histórico de versiones", description = "Devuelve el histórico de versiones inmutables de una plantilla")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Histórico obtenido exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TemplateVersionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Parámetros inválidos", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public Mono<ResponseEntity<List<TemplateVersionResponse>>> history(
            @Parameter(description = "Código funcional de la plantilla", required = true, example = "VERIFICACION_OTP")
            @PathVariable String codigo,
            @Parameter(description = "Tipo funcional de la plantilla", required = true, example = "SMS")
            @PathVariable TemplateType tipo,
            @Parameter(description = "Idioma de la plantilla", required = true, example = "es")
            @PathVariable String language) {
        return Mono.fromCallable(() -> manageTemplateUseCase.history(codigo, tipo, language))
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/{codigo}/{tipo}/{language}")
    @Operation(summary = "Desactivar plantilla", description = "Desactiva la versión activa de una plantilla (is_active = false)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Plantilla desactivada exitosamente", content = @Content),
            @ApiResponse(responseCode = "400", description = "Parámetros inválidos", content = @Content),
            @ApiResponse(responseCode = "404", description = "Plantilla no encontrada", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public Mono<ResponseEntity<Void>> deactivate(
            @Parameter(description = "Código funcional de la plantilla", required = true, example = "VERIFICACION_OTP")
            @PathVariable String codigo,
            @Parameter(description = "Tipo funcional de la plantilla", required = true, example = "SMS")
            @PathVariable TemplateType tipo,
            @Parameter(description = "Idioma de la plantilla", required = true, example = "es")
            @PathVariable String language) {
        return Mono.fromRunnable(() -> manageTemplateUseCase.deactivate(codigo, tipo, language))
                .thenReturn(ResponseEntity.noContent().build());
    }
}
