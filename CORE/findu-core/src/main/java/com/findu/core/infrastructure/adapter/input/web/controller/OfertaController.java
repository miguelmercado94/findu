package com.findu.core.infrastructure.adapter.input.web.controller;

import com.findu.core.application.dto.request.CrearOfertaRequest;
import com.findu.core.application.dto.response.OfertaProveedorDetalleResponse;
import com.findu.core.application.dto.response.OfertaResponse;
import com.findu.core.application.dto.response.SolicitudResponse;
import com.findu.core.application.port.in.GestionOfertasUseCase;
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
@RequestMapping("/api/v1/ofertas")
@RequiredArgsConstructor
@Tag(name = "Ofertas", description = "Sistema de ofertas y matching entre proveedores y solicitudes")
public class OfertaController {

    private final GestionOfertasUseCase ofertasUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Enviar oferta", description = "Permite a un proveedor enviar o reenviar una oferta para una solicitud de servicio")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Oferta enviada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OfertaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de oferta inválidos", content = @Content),
            @ApiResponse(responseCode = "404", description = "Solicitud o proveedor no encontrado", content = @Content),
            @ApiResponse(responseCode = "409", description = "Conflicto de horario (solapamiento) o la solicitud no acepta ofertas", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public ResponseEntity<OfertaResponse> enviarOferta(
            @Valid @RequestBody CrearOfertaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ofertasUseCase.enviarOferta(request));
    }

    @RequestMapping(value = {"/{id}", "/{id}/reenviar"}, method = {RequestMethod.PUT, RequestMethod.POST})
    @Operation(summary = "Editar / Reenviar oferta", description = "Permite al proveedor editar y reenviar su oferta (reiniciando los 10 minutos de vigencia y limpiando las banderas de modificación tras revisar cambios del cliente)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Oferta actualizada y reenviada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OfertaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de oferta inválidos", content = @Content),
            @ApiResponse(responseCode = "404", description = "Oferta o solicitud no encontrada", content = @Content),
            @ApiResponse(responseCode = "409", description = "Conflicto de horario (solapamiento) o estado inválido", content = @Content)
    })
    public ResponseEntity<OfertaResponse> actualizarOReenviarOferta(
            @Parameter(description = "ID de la oferta", required = true, example = "1")
            @PathVariable Long id,
            @Valid @RequestBody CrearOfertaRequest request) {
        return ResponseEntity.ok(ofertasUseCase.actualizarOferta(id, request));
    }

    @RequestMapping(value = {"/{id}", "/{id}/retirar"}, method = {RequestMethod.DELETE, RequestMethod.PATCH})
    @Operation(summary = "Retirar oferta", description = "Permite al proveedor retirar su oferta activa, haciendo que desaparezca de la pestaña 'Ofertas'")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Oferta retirada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Oferta no encontrada", content = @Content),
            @ApiResponse(responseCode = "409", description = "La oferta ya fue aceptada y no puede retirarse", content = @Content)
    })
    public ResponseEntity<Map<String, Object>> retirarOferta(
            @Parameter(description = "ID de la oferta", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "ID del perfil de proveedor (opcional para validar propiedad)", example = "1")
            @RequestParam(required = false) Long proveedorId) {
        ofertasUseCase.retirarOferta(id, proveedorId);
        return ResponseEntity.ok(Map.of(
                "ofertaId", id,
                "estadoOferta", "RETIRADA",
                "mensaje", "Oferta retirada exitosamente"
        ));
    }

    @PatchMapping("/{id}/aceptar")
    @Operation(summary = "Aceptar oferta", description = "El cliente acepta una oferta recibida, validando previamente que no exista solapamiento horario y asignando el proveedor a la solicitud")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Oferta aceptada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OfertaResponse.class))),
            @ApiResponse(responseCode = "404", description = "Oferta no encontrada", content = @Content),
            @ApiResponse(responseCode = "409", description = "La oferta expiró, no está en estado ENVIADA o presenta solapamiento horario con otro servicio del proveedor", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public ResponseEntity<OfertaResponse> aceptarOferta(
            @Parameter(description = "ID de la oferta", required = true, example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(ofertasUseCase.aceptarOferta(id));
    }

    @GetMapping("/proveedor/mis-ofertas")
    @Operation(summary = "Obtener ofertas activas del proveedor (Pestaña 'Ofertas' en Mis Trabajos)",
            description = "Lista las ofertas enviadas o pendientes de reoferta del proveedor junto con el detalle completo de la solicitud, temporizador de expiración (10 min), banderas de modificación del cliente y alerta de solapamiento horario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de ofertas activas del proveedor obtenida exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OfertaProveedorDetalleResponse.class))),
            @ApiResponse(responseCode = "404", description = "Proveedor no encontrado", content = @Content)
    })
    public ResponseEntity<List<OfertaProveedorDetalleResponse>> misOfertasProveedor(
            @Parameter(description = "ID del perfil de proveedor", required = true, example = "1")
            @RequestParam Long proveedorId) {
        return ResponseEntity.ok(ofertasUseCase.obtenerMisOfertasProveedor(proveedorId));
    }

    @GetMapping("/proveedor/solicitudes-disponibles")
    @Operation(summary = "Obtener solicitudes disponibles para el proveedor", description = "Lista las solicitudes que coinciden con la cobertura y especialidades del proveedor y en las que aún no tiene una oferta activa")
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

    @GetMapping("/proveedor/mis-solicitudes")
    @Operation(summary = "Obtener historial de solicitudes del proveedor", description = "Lista las solicitudes que el proveedor atendió (vía ofertas aceptadas). El contador de 'servicios hechos' corresponde al tamaño de la lista con estado=FINALIZADA")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de solicitudes del proveedor obtenida exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SolicitudResponse.class))),
            @ApiResponse(responseCode = "400", description = "ID de proveedor inválido", content = @Content),
            @ApiResponse(responseCode = "404", description = "Proveedor no encontrado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public ResponseEntity<List<SolicitudResponse>> misSolicitudes(
            @Parameter(description = "ID del perfil de proveedor", required = true, example = "1")
            @RequestParam Long proveedorId,
            @Parameter(description = "Estado de solicitud para filtrar (opcional). Valores: ABIERTA, EN_NEGOCIACION, PROGRAMADA, EN_CURSO, FINALIZADA, CANCELADA_SIN_PENALIDAD, CANCELADA_CON_PENALIDAD", example = "FINALIZADA")
            @RequestParam(required = false) String estado) {
        return ResponseEntity.ok(ofertasUseCase.obtenerMisSolicitudes(proveedorId, estado));
    }
}
