package com.findu.core.presentation.controller;

import com.findu.core.application.usecase.GestionDireccionesUseCase;
import com.findu.core.application.usecase.GestionPerfilClienteUseCase;
import com.findu.core.dto.request.ActualizarPerfilClienteRequest;
import com.findu.core.dto.request.CrearDireccionClienteRequest;
import com.findu.core.dto.request.CrearPerfilClienteRequest;
import com.findu.core.dto.response.DireccionResponse;
import com.findu.core.dto.response.PerfilClienteDetalleResponse;
import com.findu.core.dto.response.PerfilClienteResponse;
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
@RequestMapping("/api/v1/perfil-cliente")
@RequiredArgsConstructor
@Tag(name = "Perfil Cliente", description = "Gestión del perfil de clientes")
public class PerfilClienteController {

    private final GestionPerfilClienteUseCase perfilClienteUseCase;
    private final GestionDireccionesUseCase direccionesUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear perfil de cliente", description = "Registra un nuevo perfil de cliente en el sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Perfil de cliente creado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PerfilClienteResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content),
            @ApiResponse(responseCode = "409", description = "Ya existe un perfil para este usuario", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public ResponseEntity<PerfilClienteResponse> crearPerfil(
            @Valid @RequestBody CrearPerfilClienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(perfilClienteUseCase.crearPerfil(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar perfil de cliente por ID", description = "Obtiene el detalle completo de un perfil de cliente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Perfil de cliente encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PerfilClienteDetalleResponse.class))),
            @ApiResponse(responseCode = "404", description = "Perfil de cliente no encontrado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public ResponseEntity<PerfilClienteDetalleResponse> consultarPerfil(
            @Parameter(description = "ID del perfil de cliente", required = true, example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(perfilClienteUseCase.consultarPerfil(id));
    }

    @GetMapping("/usuario/{authUserId}")
    @Operation(summary = "Consultar perfil de cliente por Auth User ID", description = "Obtiene el detalle completo de un perfil de cliente usando su ID de autenticación")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Perfil de cliente encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PerfilClienteDetalleResponse.class))),
            @ApiResponse(responseCode = "404", description = "Perfil de cliente no encontrado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public ResponseEntity<PerfilClienteDetalleResponse> consultarPerfilPorAuthUserId(
            @Parameter(description = "ID del usuario de autenticación", required = true, example = "4")
            @PathVariable Long authUserId) {
        return ResponseEntity.ok(perfilClienteUseCase.consultarPerfilPorAuthUserId(authUserId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar perfil de cliente", description = "Actualiza los datos del perfil de un cliente existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Perfil actualizado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PerfilClienteResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content),
            @ApiResponse(responseCode = "404", description = "Perfil de cliente no encontrado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public ResponseEntity<PerfilClienteResponse> actualizarPerfil(
            @Parameter(description = "ID del perfil de cliente", required = true, example = "1")
            @PathVariable Long id,
            @RequestBody ActualizarPerfilClienteRequest request) {
        return ResponseEntity.ok(perfilClienteUseCase.actualizarPerfil(id, request));
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Cambiar estado del perfil de cliente", description = "Activa o desactiva el perfil del cliente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estado del perfil actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Estado proporcionado inválido", content = @Content),
            @ApiResponse(responseCode = "404", description = "Perfil de cliente no encontrado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public ResponseEntity<Void> cambiarEstado(
            @Parameter(description = "ID del perfil de cliente", required = true, example = "1")
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        perfilClienteUseCase.cambiarEstado(id, body.get("estado"));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/direcciones")
    @Operation(summary = "Listar direcciones del cliente", description = "Obtiene todas las direcciones registradas para un cliente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de direcciones obtenida exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = DireccionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Perfil de cliente no encontrado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public ResponseEntity<List<DireccionResponse>> listarDirecciones(
            @Parameter(description = "ID del perfil de cliente", required = true, example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(direccionesUseCase.listarDireccionesCliente(id));
    }

    @PostMapping("/{id}/direcciones")
    @Operation(summary = "Crear dirección para el cliente", description = "Registra una nueva dirección asociada al perfil del cliente. Si es la dirección principal y el perfil está INCOMPLETO, lo activa automáticamente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Dirección creada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = DireccionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de dirección inválidos", content = @Content),
            @ApiResponse(responseCode = "404", description = "Perfil de cliente o municipio no encontrado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public ResponseEntity<DireccionResponse> crearDireccion(
            @Parameter(description = "ID del perfil de cliente", required = true, example = "1")
            @PathVariable Long id,
            @Valid @RequestBody CrearDireccionClienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(direccionesUseCase.crearDireccionCliente(id, request));
    }
}
