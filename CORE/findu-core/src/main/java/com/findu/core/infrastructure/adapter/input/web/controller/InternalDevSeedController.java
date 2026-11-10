package com.findu.core.infrastructure.adapter.input.web.controller;

import com.findu.core.application.port.out.persistence.ClienteProveedorFavoritoRepositoryPort;
import com.findu.core.application.port.out.persistence.PerfilClienteRepositoryPort;
import com.findu.core.application.port.out.persistence.PerfilProveedorRepositoryPort;
import com.findu.core.domain.exception.ResourceNotFoundException;
import com.findu.core.domain.model.ClienteProveedorFavorito;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Siembra SOLO para desarrollo: crea o actualiza un vinculo cliente-proveedor. Apagado por defecto
 * (findu.dev-seed.enabled=false => 404). Ademas exige X-Internal-Service-Key (401 si falta, 403 si es incorrecta).
 */
@Hidden
@RestController
@RequestMapping("/internal/dev/mis-clientes")
@RequiredArgsConstructor
public class InternalDevSeedController {

    private static final String HEADER_INTERNAL_KEY = "X-Internal-Service-Key";

    private final ClienteProveedorFavoritoRepositoryPort favoritoPort;
    private final PerfilClienteRepositoryPort clientePort;
    private final PerfilProveedorRepositoryPort proveedorPort;

    @Value("${findu.dev-seed.enabled:false}")
    private boolean enabled;

    @Value("${findu.internal.service-key:findu-internal-saga-secret-key-2026}")
    private String expectedInternalKey;

    public record VinculoSeedRequest(Long perfilClienteId, Long perfilProveedorId, Boolean activo) {
    }

    public record VinculoSeedResponse(UUID clienteRef, boolean activo) {
    }

    @PostMapping("/vinculo")
    @Transactional
    public ResponseEntity<VinculoSeedResponse> vincular(
            @RequestHeader(name = HEADER_INTERNAL_KEY, required = false) String serviceKey,
            @RequestBody VinculoSeedRequest body) {
        if (!enabled) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No encontrado");
        }
        if (serviceKey == null || serviceKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Llave interna requerida");
        }
        if (!serviceKey.equals(expectedInternalKey)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso interno no autorizado");
        }
        if (body == null || body.perfilClienteId() == null || body.perfilProveedorId() == null) {
            throw new IllegalArgumentException("perfilClienteId y perfilProveedorId son requeridos");
        }
        if (clientePort.findById(body.perfilClienteId()).isEmpty()) {
            throw new ResourceNotFoundException("Cliente", body.perfilClienteId());
        }
        if (proveedorPort.findById(body.perfilProveedorId()).isEmpty()) {
            throw new ResourceNotFoundException("Proveedor", body.perfilProveedorId());
        }
        boolean activo = body.activo() == null || body.activo();
        ClienteProveedorFavorito vinculo = favoritoPort
                .findByClienteAndProveedor(body.perfilClienteId(), body.perfilProveedorId())
                .orElseGet(() -> ClienteProveedorFavorito.builder()
                        .perfilClienteId(body.perfilClienteId())
                        .perfilProveedorId(body.perfilProveedorId())
                        .clienteRef(UUID.randomUUID())
                        .favoritoDesde(LocalDateTime.now())
                        .build());
        vinculo.setActivo(activo);
        vinculo.setRemovidoAt(activo ? null : LocalDateTime.now());
        ClienteProveedorFavorito guardado = favoritoPort.save(vinculo);
        return ResponseEntity.ok(new VinculoSeedResponse(guardado.getClienteRef(), guardado.isActivo()));
    }
}