package com.findu.core.presentation.controller;

import com.findu.core.application.usecase.GestionCalificacionesUseCase;
import com.findu.core.dto.request.CrearCalificacionRequest;
import com.findu.core.dto.response.CalificacionResponse;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/calificaciones")
@RequiredArgsConstructor
@Tag(name = "Calificaciones", description = "Sistema de calificación y reputación")
public class CalificacionController {

    private final GestionCalificacionesUseCase calificacionesUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Calificar una solicitud finalizada", description = "Permite al cliente o proveedor calificar una solicitud de servicio completada")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Calificación registrada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CalificacionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de calificación inválidos", content = @Content),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada", content = @Content),
            @ApiResponse(responseCode = "409", description = "La solicitud ya fue calificada o no está en estado finalizado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public ResponseEntity<CalificacionResponse> calificar(
            @Valid @RequestBody CrearCalificacionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(calificacionesUseCase.calificar(request));
    }

    @GetMapping("/{perfilId}")
    @Operation(summary = "Consultar reputación de un perfil", description = "Obtiene todas las calificaciones recibidas por un perfil de usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Calificaciones obtenidas exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CalificacionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Perfil no encontrado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public ResponseEntity<List<CalificacionResponse>> consultarReputacion(
            @Parameter(description = "ID del perfil de usuario (cliente o proveedor)", required = true, example = "1")
            @PathVariable Long perfilId) {
        return ResponseEntity.ok(calificacionesUseCase.consultarReputacion(perfilId));
    }
}
