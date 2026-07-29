package com.findu.core.presentation.controller;

import com.findu.core.application.usecase.GestionOfertasUseCase;
import com.findu.core.dto.request.CrearOfertaRequest;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ofertas")
@RequiredArgsConstructor
@Tag(name = "Ofertas", description = "Sistema de ofertas y matching entre proveedores y solicitudes")
public class OfertaController {

    private final GestionOfertasUseCase ofertasUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Enviar oferta", description = "Permite a un proveedor enviar una oferta para una solicitud de servicio")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Oferta enviada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OfertaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de oferta inválidos", content = @Content),
            @ApiResponse(responseCode = "404", description = "Solicitud o proveedor no encontrado", content = @Content),
            @ApiResponse(responseCode = "409", description = "El proveedor ya envió una oferta para esta solicitud o la solicitud no acepta ofertas", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public ResponseEntity<OfertaResponse> enviarOferta(
            @Valid @RequestBody CrearOfertaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ofertasUseCase.enviarOferta(request));
    }

    @PatchMapping("/{id}/aceptar")
    @Operation(summary = "Aceptar oferta", description = "El cliente acepta una oferta recibida, asignando el proveedor a la solicitud")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Oferta aceptada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OfertaResponse.class))),
            @ApiResponse(responseCode = "404", description = "Oferta no encontrada", content = @Content),
            @ApiResponse(responseCode = "409", description = "La oferta no puede ser aceptada en su estado actual", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public ResponseEntity<OfertaResponse> aceptarOferta(
            @Parameter(description = "ID de la oferta", required = true, example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(ofertasUseCase.aceptarOferta(id));
    }

    @GetMapping("/proveedor/solicitudes-disponibles")
    @Operation(summary = "Obtener solicitudes disponibles para el proveedor", description = "Lista las solicitudes que coinciden con la cobertura y especialidades del proveedor")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de solicitudes disponibles obtenida exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SolicitudResponse.class))),
            @ApiResponse(responseCode = "400", description = "ID de proveedor inválido", content = @Content),
            @ApiResponse(responseCode = "404", description = "Proveedor no encontrado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public ResponseEntity<List<SolicitudResponse>> solicitudesDisponibles(
            @Parameter(description = "ID del perfil de proveedor", required = true, example = "1")
            @RequestParam Long proveedorId) {
        return ResponseEntity.ok(ofertasUseCase.obtenerSolicitudesDisponibles(proveedorId));
    }
}
