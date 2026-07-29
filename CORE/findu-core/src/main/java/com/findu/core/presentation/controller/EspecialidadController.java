package com.findu.core.presentation.controller;

import com.findu.core.application.usecase.GestionEspecialidadesUseCase;
import com.findu.core.dto.request.CrearEspecialidadRequest;
import com.findu.core.dto.request.CrearPortafolioRequest;
import com.findu.core.dto.response.PerfilEspecialistaResponse;
import com.findu.core.dto.response.PortafolioItemResponse;
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

import java.util.Map;

@RestController
@RequestMapping("/api/v1/perfiles-especialistas")
@RequiredArgsConstructor
@Tag(name = "Especialidades y Portafolio", description = "Gestión de especialidades y portafolio del proveedor")
public class EspecialidadController {

    private final GestionEspecialidadesUseCase especialidadesUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Agregar especialidad", description = "Registra una nueva especialidad para un proveedor")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Especialidad agregada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PerfilEspecialistaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content),
            @ApiResponse(responseCode = "404", description = "Proveedor o servicio no encontrado", content = @Content),
            @ApiResponse(responseCode = "409", description = "La especialidad ya existe para este proveedor", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public ResponseEntity<PerfilEspecialistaResponse> agregarEspecialidad(
            @Valid @RequestBody CrearEspecialidadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(especialidadesUseCase.agregarEspecialidad(request));
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Cambiar estado de especialidad", description = "Activa o desactiva una especialidad del proveedor")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estado de especialidad actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Valor de estado inválido", content = @Content),
            @ApiResponse(responseCode = "404", description = "Especialidad no encontrada", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public ResponseEntity<Void> cambiarEstado(
            @Parameter(description = "ID de la especialidad", required = true, example = "1")
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body) {
        especialidadesUseCase.cambiarEstadoEspecialidad(id, body.get("active"));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar especialidad", description = "Elimina una especialidad del proveedor de forma permanente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Especialidad eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Especialidad no encontrada", content = @Content),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar la especialidad porque tiene solicitudes activas", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public ResponseEntity<Void> eliminarEspecialidad(
            @Parameter(description = "ID de la especialidad", required = true, example = "1")
            @PathVariable Long id) {
        especialidadesUseCase.eliminarEspecialidad(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/portafolio")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Agregar item al portafolio", description = "Agrega un nuevo trabajo realizado al portafolio de una especialidad")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Item de portafolio agregado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PortafolioItemResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content),
            @ApiResponse(responseCode = "404", description = "Especialidad no encontrada", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public ResponseEntity<PortafolioItemResponse> agregarPortafolio(
            @Parameter(description = "ID de la especialidad", required = true, example = "1")
            @PathVariable Long id,
            @Valid @RequestBody CrearPortafolioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(especialidadesUseCase.agregarPortafolio(id, request));
    }

    @DeleteMapping("/portafolio/{itemId}")
    @Operation(summary = "Eliminar item del portafolio", description = "Elimina un trabajo del portafolio de una especialidad")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Item de portafolio eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Item de portafolio no encontrado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public ResponseEntity<Void> eliminarPortafolio(
            @Parameter(description = "ID del item de portafolio", required = true, example = "1")
            @PathVariable Long itemId) {
        especialidadesUseCase.eliminarPortafolio(itemId);
        return ResponseEntity.noContent().build();
    }
}
