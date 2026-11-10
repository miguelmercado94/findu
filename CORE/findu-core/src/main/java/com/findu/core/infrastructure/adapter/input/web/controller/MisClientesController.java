package com.findu.core.infrastructure.adapter.input.web.controller;

import com.findu.core.application.dto.response.ClienteDetalleResponse;
import com.findu.core.application.dto.response.ClienteResumenResponse;
import com.findu.core.application.dto.response.PaginaResponse;
import com.findu.core.application.dto.response.ServicioHistorialItemResponse;
import com.findu.core.application.port.in.GestionMisClientesUseCase;
import com.findu.core.application.port.in.GestionMisClientesUseCase.FotoCliente;
import com.findu.core.infrastructure.security.CurrentActorResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * "Mis clientes" del proveedor. El proveedor sale SOLO del JWT ({@link CurrentActorResolver}); ningun endpoint
 * recibe un id de proveedor ni de cliente real: el cliente se identifica por el {@code clienteRef} opaco.
 */
@RestController
@RequestMapping("/api/v1/proveedor/mis-clientes")
@RequiredArgsConstructor
@Tag(name = "Mis clientes", description = "Clientes que tienen al proveedor como favorito")
public class MisClientesController {

    private final GestionMisClientesUseCase useCase;
    private final CurrentActorResolver actorResolver;

    public record FavoritoInternoRequest(Boolean favorito) {
    }

    @GetMapping
    @Operation(summary = "Lista paginada de clientes vinculados (en servicio, favoritos internos, resto)")
    public ResponseEntity<PaginaResponse<ClienteResumenResponse>> listar(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        Long proveedorId = actorResolver.requirePerfilProveedorId(request);
        return ResponseEntity.ok(useCase.listar(proveedorId, q, page, size));
    }

    @GetMapping("/{clienteRef}")
    @Operation(summary = "Detalle de un cliente vinculado (404 sin vinculo)")
    public ResponseEntity<ClienteDetalleResponse> detalle(@PathVariable UUID clienteRef, HttpServletRequest request) {
        Long proveedorId = actorResolver.requirePerfilProveedorId(request);
        return ResponseEntity.ok(useCase.detalle(proveedorId, clienteRef));
    }

    @GetMapping("/{clienteRef}/servicios")
    @Operation(summary = "Historial de servicios con este cliente (sin codigo de ticket)")
    public ResponseEntity<PaginaResponse<ServicioHistorialItemResponse>> servicios(
            @PathVariable UUID clienteRef,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        Long proveedorId = actorResolver.requirePerfilProveedorId(request);
        return ResponseEntity.ok(useCase.historial(proveedorId, clienteRef, page, size));
    }

    @GetMapping("/{clienteRef}/foto")
    @Operation(summary = "Bytes de la foto de perfil del cliente")
    public ResponseEntity<byte[]> foto(@PathVariable UUID clienteRef, HttpServletRequest request) {
        Long proveedorId = actorResolver.requirePerfilProveedorId(request);
        FotoCliente foto = useCase.foto(proveedorId, clienteRef);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(foto.contentType()))
                .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).cachePrivate())
                .body(foto.bytes());
    }

    @PutMapping("/{clienteRef}/favorito-interno")
    @Operation(summary = "Marca o desmarca al cliente como favorito interno del proveedor (idempotente)")
    public ResponseEntity<Void> favoritoInterno(@PathVariable UUID clienteRef,
                                                @RequestBody FavoritoInternoRequest body,
                                                HttpServletRequest request) {
        Long proveedorId = actorResolver.requirePerfilProveedorId(request);
        if (body == null || body.favorito() == null) {
            throw new IllegalArgumentException("El campo 'favorito' es requerido");
        }
        useCase.definirFavoritoInterno(proveedorId, clienteRef, body.favorito());
        return ResponseEntity.noContent().build();
    }
}