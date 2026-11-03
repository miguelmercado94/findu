package com.findu.core.infrastructure.adapter.input.web.controller;

import com.findu.core.application.dto.request.CrearSolicitudRequest;
import com.findu.core.application.dto.request.IniciarServicioRequest;
import com.findu.core.application.dto.request.ModificarSolicitudRequest;
import com.findu.core.application.dto.response.DireccionResponse;
import com.findu.core.application.dto.response.OfertaResponse;
import com.findu.core.application.dto.response.SolicitudResponse;
import com.findu.core.application.port.in.GestionDireccionesUseCase;
import com.findu.core.application.port.in.GestionOfertasUseCase;
import com.findu.core.application.port.in.GestionSolicitudesUseCase;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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
    @Operation(summary = "Consultar historial de solicitudes (paginado)", description = "Obtiene las solicitudes de un cliente con paginación, filtrable por estado. Por defecto muestra las ABIERTAS. Incluye el PIN de 4 dígitos cuando la solicitud está ACEPTADA/PROGRAMADA.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Historial de solicitudes obtenido exitosamente"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public ResponseEntity<Page<SolicitudResponse>> consultarHistorial(
            @Parameter(description = "ID del perfil de cliente", required = true, example = "1")
            @PathVariable Long clienteId,
            @Parameter(description = "Filtrar por estado (ABIERTA, EN_NEGOCIACION, PROGRAMADA, ACEPTADA, EN_CURSO, FINALIZADA, CANCELADO). Si no se envía, muestra ABIERTA.", example = "ABIERTA")
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
    @Operation(summary = "Cancelar solicitud (Cliente)", description = "Cancela una solicitud de servicio por parte del cliente")
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

    @RequestMapping(value = "/{id}/reabrir", method = {RequestMethod.POST, RequestMethod.PATCH})
    @Operation(summary = "Reabrir solicitud tras cancelación del proveedor",
            description = "Permite al cliente decidir reabrir su solicitud después de que un proveedor canceló el servicio, volviendo a estado ABIERTA para recibir ofertas de otros proveedores")
    public ResponseEntity<SolicitudResponse> reabrirSolicitud(
            @PathVariable Long id,
            @RequestParam(required = false) BigDecimal latitud,
            @RequestParam(required = false) BigDecimal longitud) {
        return ResponseEntity.ok(solicitudesUseCase.reabrirSolicitudCliente(id, latitud, longitud));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener detalle de solicitud", description = "Obtiene los detalles completos de una solicitud por su ID. Si se envía clienteId del titular, incluye el PIN de 4 dígitos.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Solicitud encontrada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SolicitudResponse.class))),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada", content = @Content)
    })
    public ResponseEntity<SolicitudResponse> obtenerSolicitudPorId(
            @Parameter(description = "ID de la solicitud", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "ID del cliente titular (opcional; si coincide, expone pinInicio)")
            @RequestParam(required = false) Long clienteId) {
        if (clienteId != null) {
            return ResponseEntity.ok(solicitudesUseCase.obtenerSolicitudParaCliente(id, clienteId));
        }
        return ResponseEntity.ok(solicitudesUseCase.obtenerSolicitudPorId(id));
    }

    @GetMapping("/{id}/pin-inicio")
    @Operation(summary = "Consultar PIN de 4 dígitos para iniciar el servicio (Exclusivo Cliente)",
            description = "Retorna el PIN de 4 dígitos que el cliente debe dictarle al proveedor para pasar de ACEPTADA a EN_CURSO. En perfil dev es '1234'; en QA y PDN se desencripta exclusivamente para el cliente titular.")
    public ResponseEntity<Map<String, Object>> obtenerPinInicioCliente(
            @PathVariable Long id,
            @RequestParam Long clienteId) {
        String pin = solicitudesUseCase.obtenerPinInicioParaCliente(id, clienteId);
        return ResponseEntity.ok(Map.of(
                "solicitudId", id,
                "pinInicio", pin != null ? pin : ""
        ));
    }

    @RequestMapping(value = "/{id}/iniciar", method = {RequestMethod.POST, RequestMethod.PATCH})
    @Operation(summary = "Iniciar servicio (ACEPTADA -> EN_CURSO con PIN de 4 dígitos y GPS <= 20m)",
            description = "Valida el PIN de 4 dígitos brindado por el cliente, verifica que la posición GPS esté a máximo 20 metros de la dirección del servicio, ejecuta el seguro final de no solapamiento horario y guarda auditoría interna de hora y GPS")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Servicio iniciado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SolicitudResponse.class))),
            @ApiResponse(responseCode = "400", description = "PIN inválido o coordenadas GPS faltantes", content = @Content),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada", content = @Content),
            @ApiResponse(responseCode = "409", description = "Distancia GPS mayor a 20 metros, solapamiento horario o estado inválido", content = @Content)
    })
    public ResponseEntity<SolicitudResponse> iniciarServicio(
            @Parameter(description = "ID de la solicitud", required = true, example = "1")
            @PathVariable Long id,
            @RequestParam(required = false) String pin,
            @RequestParam(required = false) BigDecimal latitud,
            @RequestParam(required = false) BigDecimal longitud,
            @RequestParam(required = false) Long actorId,
            @RequestBody(required = false) IniciarServicioRequest body) {

        IniciarServicioRequest effectiveRequest = body;
        if (effectiveRequest == null && (pin != null || latitud != null || longitud != null || actorId != null)) {
            effectiveRequest = new IniciarServicioRequest(pin, latitud, longitud, actorId);
        }
        return ResponseEntity.ok(solicitudesUseCase.iniciarServicio(id, effectiveRequest));
    }

    @RequestMapping(value = "/{id}/finalizar", method = {RequestMethod.POST, RequestMethod.PUT})
    @Operation(summary = "Finalizar solicitud", description = "Finaliza el servicio, guarda auditoría interna de hora/GPS y registra la transacción financiera en findu-transaction")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Solicitud finalizada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SolicitudResponse.class))),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public ResponseEntity<SolicitudResponse> finalizarServicio(
            @Parameter(description = "ID de la solicitud", required = true, example = "1")
            @PathVariable Long id,
            @RequestParam(required = false) BigDecimal latitud,
            @RequestParam(required = false) BigDecimal longitud) {
        return ResponseEntity.ok(solicitudesUseCase.finalizarServicio(id, latitud, longitud));
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
