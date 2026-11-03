package com.findu.core.infrastructure.adapter.input.web.controller;

import com.findu.core.application.dto.request.AdjuntarEvidenciaCancelacionRequest;
import com.findu.core.application.dto.request.ResolverCancelacionSoporteRequest;
import com.findu.core.application.dto.request.SolicitarCancelacionProveedorRequest;
import com.findu.core.application.dto.response.CancelacionProveedorResponse;
import com.findu.core.application.dto.response.CategoriaCancelacionCatalogoResponse;
import com.findu.core.application.dto.response.HistorialDisciplinarioProveedorResponse;
import com.findu.core.application.dto.response.PrevisualizacionCancelacionResponse;
import com.findu.core.application.port.in.GestionCancelacionesProveedorUseCase;
import com.findu.core.domain.enums.CategoriaCancelacionProveedor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cancelaciones")
@RequiredArgsConstructor
@Tag(name = "Cancelaciones y Amonestaciones de Proveedores",
        description = "Flujo de cancelaciones por categorías (1 a 4), tickets de soporte, plazos de evidencia y sistema de amonestaciones en ventana móvil de 30 días")
public class CancelacionProveedorController {

    private final GestionCancelacionesProveedorUseCase cancelacionesUseCase;

    @GetMapping("/categorias")
    @Operation(summary = "Listar las 4 categorías de cancelación y sus motivos",
            description = "Retorna el catálogo de las 4 categorías de cancelación para proveedores junto con sus motivos específicos y textos de advertencia base")
    public ResponseEntity<List<CategoriaCancelacionCatalogoResponse>> listarCategorias() {
        return ResponseEntity.ok(cancelacionesUseCase.obtenerCatalogoCategorias());
    }

    @GetMapping("/proveedor/previsualizar")
    @Operation(summary = "Previsualizar advertencia y proyección de amonestación antes de cancelar",
            description = "Consulta en tiempo real el historial de los últimos 30 días del proveedor y retorna la advertencia exacta y el nivel de amonestación que alcanzaría si confirma la cancelación")
    public ResponseEntity<PrevisualizacionCancelacionResponse> previsualizarCancelacion(
            @RequestParam Long proveedorId,
            @RequestParam CategoriaCancelacionProveedor categoria) {
        return ResponseEntity.ok(cancelacionesUseCase.previsualizarCancelacion(proveedorId, categoria));
    }

    @PostMapping("/proveedor")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Solicitar o confirmar cancelación de servicio por el proveedor",
            description = "Procesa la solicitud de cancelación según la categoría seleccionada (Categoría 1: pendiente de soporte; Categoría 2: fuerza mayor con 3 días para evidencia; Categoría 3: cancelación inmediata con evaluación en ventana móvil de 30 días; Categoría 4: otros motivos con descripción obligatoria y evaluación de soporte)")
    public ResponseEntity<CancelacionProveedorResponse> solicitarCancelacion(
            @Valid @RequestBody SolicitarCancelacionProveedorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cancelacionesUseCase.solicitarCancelacion(request));
    }

    @RequestMapping(value = "/{id}/evidencias", method = {RequestMethod.POST, RequestMethod.PATCH})
    @Operation(summary = "Adjuntar soportes o evidencias a una solicitud de cancelación (Categoría 2 Fuerza Mayor)",
            description = "Permite al proveedor cargar documentos, fotografías u otros soportes posteriormente desde su ticket antes de que venza el plazo de 3 días")
    public ResponseEntity<CancelacionProveedorResponse> adjuntarEvidencias(
            @PathVariable Long id,
            @Valid @RequestBody AdjuntarEvidenciaCancelacionRequest request) {
        return ResponseEntity.ok(cancelacionesUseCase.adjuntarEvidencias(id, request));
    }

    @RequestMapping(value = "/{id}/resolver-soporte", method = {RequestMethod.POST, RequestMethod.PATCH})
    @Operation(summary = "Resolver solicitud/ticket de cancelación desde Soporte",
            description = "Permite a soporte aprobar o rechazar la solicitud de cancelación (Categoría 1), validar o rechazar evidencias de fuerza mayor (Categoría 2), o dictaminar si una cancelación de Categoría 4 es justificada o injustificada")
    public ResponseEntity<CancelacionProveedorResponse> resolverPorSoporte(
            @PathVariable Long id,
            @Valid @RequestBody ResolverCancelacionSoporteRequest request) {
        return ResponseEntity.ok(cancelacionesUseCase.resolverPorSoporte(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar detalle de una cancelación por su ID")
    public ResponseEntity<CancelacionProveedorResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(cancelacionesUseCase.obtenerCancelacionPorId(id));
    }

    @GetMapping("/ticket/{ticketCodigo}")
    @Operation(summary = "Consultar detalle de una cancelación por el código de ticket de soporte")
    public ResponseEntity<CancelacionProveedorResponse> obtenerPorTicket(@PathVariable String ticketCodigo) {
        return ResponseEntity.ok(cancelacionesUseCase.obtenerCancelacionPorTicket(ticketCodigo));
    }

    @GetMapping({"/proveedor/historial-disciplinario", "/proveedor/amonestaciones"})
    @Operation(summary = "Consultar amonestaciones, restricciones activas e historial de cancelaciones del proveedor",
            description = "Retorna el conteo de cancelaciones injustificadas de los últimos 30 días, si posee restricción vigente, los días/horas restantes de restricción, y la lista histórica de amonestaciones y cancelaciones")
    public ResponseEntity<HistorialDisciplinarioProveedorResponse> obtenerHistorialDisciplinario(
            @RequestParam Long proveedorId) {
        return ResponseEntity.ok(cancelacionesUseCase.obtenerHistorialDisciplinario(proveedorId));
    }
}

