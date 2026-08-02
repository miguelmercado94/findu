package com.findu.core.presentation.controller;

import com.findu.core.application.usecase.GestionPerfilProveedorUseCase;
import com.findu.core.dto.request.ActualizarPerfilProveedorRequest;
import com.findu.core.dto.request.CrearPerfilProveedorRequest;
import com.findu.core.dto.response.PerfilProveedorDetalleResponse;
import com.findu.core.dto.response.PerfilProveedorPublicoResponse;
import com.findu.core.dto.response.PerfilProveedorResponse;
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
import java.util.Map;

@RestController
@RequestMapping("/api/v1/perfil-proveedor")
@RequiredArgsConstructor
@Tag(name = "Perfil Proveedor", description = "Gestión del perfil de proveedores")
public class PerfilProveedorController {

    private final GestionPerfilProveedorUseCase proveedorUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear perfil de proveedor", description = "Registra un nuevo perfil de proveedor en el sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Perfil de proveedor creado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PerfilProveedorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content),
            @ApiResponse(responseCode = "409", description = "Ya existe un perfil de proveedor para este usuario", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public ResponseEntity<PerfilProveedorResponse> crearPerfil(
            @Valid @RequestBody CrearPerfilProveedorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(proveedorUseCase.crearPerfil(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar perfil de proveedor por ID", description = "Obtiene el detalle completo de un perfil de proveedor incluyendo especialidades y cobertura")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Perfil de proveedor encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PerfilProveedorDetalleResponse.class))),
            @ApiResponse(responseCode = "404", description = "Perfil de proveedor no encontrado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public ResponseEntity<PerfilProveedorDetalleResponse> consultarPerfil(
            @Parameter(description = "ID del perfil de proveedor", required = true, example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(proveedorUseCase.consultarPerfil(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar perfil de proveedor", description = "Actualiza los datos del perfil de un proveedor existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Perfil de proveedor actualizado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PerfilProveedorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content),
            @ApiResponse(responseCode = "404", description = "Perfil de proveedor no encontrado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public ResponseEntity<PerfilProveedorResponse> actualizarPerfil(
            @Parameter(description = "ID del perfil de proveedor", required = true, example = "1")
            @PathVariable Long id,
            @RequestBody ActualizarPerfilProveedorRequest request) {
        return ResponseEntity.ok(proveedorUseCase.actualizarPerfil(id, request));
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Cambiar estado del perfil de proveedor", description = "Activa o desactiva el perfil del proveedor")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estado del perfil actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Estado proporcionado inválido", content = @Content),
            @ApiResponse(responseCode = "404", description = "Perfil de proveedor no encontrado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public ResponseEntity<Void> cambiarEstado(
            @Parameter(description = "ID del perfil de proveedor", required = true, example = "1")
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        proveedorUseCase.cambiarEstado(id, body.get("estado"));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/cobertura")
    @Operation(summary = "Actualizar cobertura del proveedor", description = "Define los municipios donde el proveedor ofrece sus servicios mediante códigos DANE")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cobertura actualizada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Códigos DANE inválidos", content = @Content),
            @ApiResponse(responseCode = "404", description = "Perfil de proveedor no encontrado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public ResponseEntity<Void> actualizarCobertura(
            @Parameter(description = "ID del perfil de proveedor", required = true, example = "1")
            @PathVariable Long id,
            @RequestBody List<String> codigosDane) {
        proveedorUseCase.actualizarCobertura(id, codigosDane);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/publico")
    @Operation(summary = "Ver perfil público del proveedor (vista cliente)", description = "Muestra el perfil del proveedor filtrado por servicio: nombre, calificación, especialidad relevante y portafolio. No expone datos sensibles.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Perfil público del proveedor",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PerfilProveedorPublicoResponse.class))),
            @ApiResponse(responseCode = "404", description = "Proveedor no encontrado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public ResponseEntity<PerfilProveedorPublicoResponse> consultarPerfilPublico(
            @Parameter(description = "ID del perfil de proveedor", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "ID del servicio para filtrar la especialidad mostrada", required = true, example = "3")
            @RequestParam Long servicioId) {
        return ResponseEntity.ok(proveedorUseCase.consultarPerfilPublico(id, servicioId));
    }
}
