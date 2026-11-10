package com.findu.core.infrastructure.adapter.input.web.controller;

import com.findu.core.application.port.in.GestionFavoritosClienteUseCase;
import com.findu.core.application.port.in.GestionFavoritosClienteUseCase.FavoritoProveedor;
import com.findu.core.infrastructure.security.CurrentActorResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Contrato para la app cliente (Chambii): marcar / quitar / consultar proveedores favoritos. */
@RestController
@RequestMapping("/api/v1/perfil-cliente/{id}/favoritos")
@RequiredArgsConstructor
@Tag(name = "Favoritos del cliente", description = "Proveedores favoritos del cliente")
public class FavoritoProveedorController {

    private final GestionFavoritosClienteUseCase useCase;
    private final CurrentActorResolver actorResolver;

    @GetMapping
    @Operation(summary = "Lista los proveedores favoritos del cliente (activos e inactivos)")
    public ResponseEntity<List<FavoritoProveedor>> listar(@PathVariable Long id, HttpServletRequest request) {
        actorResolver.requirePerfilClienteDueno(request, id);
        return ResponseEntity.ok(useCase.listar(id));
    }

    @GetMapping("/{proveedorId}")
    @Operation(summary = "Estado de favorito de un proveedor (404 si nunca se marco)")
    public ResponseEntity<FavoritoProveedor> obtener(@PathVariable Long id, @PathVariable Long proveedorId,
                                                     HttpServletRequest request) {
        actorResolver.requirePerfilClienteDueno(request, id);
        return ResponseEntity.ok(useCase.obtener(id, proveedorId));
    }

    @PutMapping("/{proveedorId}")
    @Operation(summary = "Marca o reactiva el favorito (idempotente)")
    public ResponseEntity<Void> marcar(@PathVariable Long id, @PathVariable Long proveedorId,
                                       HttpServletRequest request) {
        actorResolver.requirePerfilClienteDueno(request, id);
        useCase.marcarFavorito(id, proveedorId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{proveedorId}")
    @Operation(summary = "Quita el favorito: baja logica, conserva la fila (idempotente)")
    public ResponseEntity<Void> quitar(@PathVariable Long id, @PathVariable Long proveedorId,
                                       HttpServletRequest request) {
        actorResolver.requirePerfilClienteDueno(request, id);
        useCase.quitarFavorito(id, proveedorId);
        return ResponseEntity.noContent().build();
    }
}