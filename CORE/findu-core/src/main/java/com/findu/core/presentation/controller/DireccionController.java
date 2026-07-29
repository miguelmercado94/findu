package com.findu.core.presentation.controller;

import com.findu.core.application.usecase.GestionDireccionesUseCase;
import com.findu.core.dto.request.ActualizarDireccionRequest;
import com.findu.core.dto.request.CrearDireccionRequest;
import com.findu.core.dto.response.DireccionResponse;
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

@RestController
@RequestMapping("/api/v1/direcciones")
@RequiredArgsConstructor
@Tag(name = "Direcciones", description = "Gestión de direcciones de usuarios")
public class DireccionController {

    private final GestionDireccionesUseCase direccionesUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear dirección", description = "Registra una nueva dirección para un usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Dirección creada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = DireccionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de dirección inválidos", content = @Content),
            @ApiResponse(responseCode = "404", description = "Municipio o usuario no encontrado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public ResponseEntity<DireccionResponse> crearDireccion(
            @Valid @RequestBody CrearDireccionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(direccionesUseCase.crearDireccion(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar dirección", description = "Actualiza los datos de una dirección existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dirección actualizada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = DireccionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de dirección inválidos", content = @Content),
            @ApiResponse(responseCode = "404", description = "Dirección no encontrada", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public ResponseEntity<DireccionResponse> actualizarDireccion(
            @Parameter(description = "ID de la dirección", required = true, example = "1")
            @PathVariable Long id,
            @RequestBody ActualizarDireccionRequest request) {
        return ResponseEntity.ok(direccionesUseCase.actualizarDireccion(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar dirección", description = "Desactiva una dirección (eliminación lógica)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Dirección eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Dirección no encontrada", content = @Content),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar la dirección porque está asociada a solicitudes activas", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public ResponseEntity<Void> eliminarDireccion(
            @Parameter(description = "ID de la dirección", required = true, example = "1")
            @PathVariable Long id) {
        direccionesUseCase.eliminarDireccion(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar dirección por ID", description = "Obtiene el detalle de una dirección específica")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dirección encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = DireccionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Dirección no encontrada", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public ResponseEntity<DireccionResponse> consultarDireccion(
            @Parameter(description = "ID de la dirección", required = true, example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(direccionesUseCase.consultarDireccion(id));
    }
}
