package com.findu.core.presentation.controller;

import com.findu.core.application.usecase.GestionDireccionesUseCase;
import com.findu.core.application.usecase.GestionOfertasUseCase;
import com.findu.core.application.usecase.GestionSolicitudesUseCase;
import com.findu.core.dto.request.CrearSolicitudRequest;
import com.findu.core.dto.request.ModificarSolicitudRequest;
import com.findu.core.dto.response.DireccionResponse;
import com.findu.core.dto.response.OfertaResponse;
import com.findu.core.dto.response.SolicitudResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/solicitudes")
@RequiredArgsConstructor
@Tag(name = "Solicitudes de Servicio", description = "Gestión del ciclo de vida de solicitudes")
public class SolicitudController {

    private final GestionSolicitudesUseCase solicitudesUseCase;
    private final GestionDireccionesUseCase direccionesUseCase;
    private final GestionOfertasUseCase ofertasUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear solicitud de servicio", description = "Registra una nueva solicitud de servicio por parte de un cliente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Solicitud creada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SolicitudResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de solicitud inválidos", content = @Content),
            @ApiResponse(responseCode = "404", description = "Cliente, servicio o dirección no encontrados", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public ResponseEntity<SolicitudResponse> crearSolicitud(
            @Valid @RequestBody CrearSolicitudRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(solicitudesUseCase.crearSolicitud(request));
    }

    @GetMapping("/cliente/{clienteId}")
    @Operation(summary = "Consultar historial de solicitudes (paginado)", description = "Obtiene las solicitudes de un cliente con paginación, filtrable por estado. Por defecto muestra las ABIERTAS.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Historial de solicitudes obtenido exitosamente"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public ResponseEntity<Page<SolicitudResponse>> consultarHistorial(
            @Parameter(description = "ID del perfil de cliente", required = true, example = "1")
            @PathVariable Long clienteId,
            @Parameter(description = "Filtrar por estado (ABIERTA, EN_NEGOCIACION, PROGRAMADA, EN_CURSO, FINALIZADA, CANCELADA_SIN_PENALIDAD, CANCELADA_CON_PENALIDAD). Si no se envía, muestra ABIERTA.", example = "ABIERTA")
            @RequestParam(defaultValue = "ABIERTA") String estado,
            @Parameter(description = "Número de página (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(solicitudesUseCase.consultarHistorial(clienteId, estado, pageable));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modificar solicitud", description = "Modifica los datos de una solicitud existente que aún no ha sido congelada")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Solicitud modificada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SolicitudResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de solicitud inválidos", content = @Content),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada", content = @Content),
            @ApiResponse(responseCode = "409", description = "La solicitud no puede ser modificada en su estado actual", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public ResponseEntity<SolicitudResponse> modificarSolicitud(
            @Parameter(description = "ID de la solicitud", required = true, example = "1")
            @PathVariable Long id,
            @RequestBody ModificarSolicitudRequest request) {
        return ResponseEntity.ok(solicitudesUseCase.modificarSolicitud(id, request));
    }

    @PatchMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar solicitud", description = "Cancela una solicitud de servicio que aún no ha sido completada")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Solicitud cancelada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SolicitudResponse.class))),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada", content = @Content),
            @ApiResponse(responseCode = "409", description = "La solicitud no puede ser cancelada en su estado actual", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public ResponseEntity<SolicitudResponse> cancelarSolicitud(
            @Parameter(description = "ID de la solicitud", required = true, example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(solicitudesUseCase.cancelarSolicitud(id));
    }

    @GetMapping("/{id}/direccion")
    @Operation(summary = "Obtener dirección de la solicitud", description = "Retorna la dirección asociada a una solicitud de servicio")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dirección de la solicitud obtenida exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = DireccionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Solicitud o dirección no encontrada", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public ResponseEntity<DireccionResponse> obtenerDireccion(
            @Parameter(description = "ID de la solicitud", required = true, example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(direccionesUseCase.obtenerDireccionSolicitud(id));
    }

    @GetMapping("/{id}/ofertas")
    @Operation(summary = "Listar ofertas de una solicitud", description = "Obtiene todas las ofertas recibidas para una solicitud específica")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de ofertas obtenida exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OfertaResponse.class))),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public ResponseEntity<List<OfertaResponse>> listarOfertas(
            @Parameter(description = "ID de la solicitud", required = true, example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(ofertasUseCase.listarOfertasPorSolicitud(id));
    }
}
