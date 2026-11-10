package com.findu.core.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/** Vinculo cliente -> proveedor favorito. Nunca se borra: la baja pone activo=false. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClienteProveedorFavorito {
    private Long id;
    private Long perfilClienteId;
    private Long perfilProveedorId;
    /** Id opaco por vinculo que expone la API del proveedor (nunca el id del cliente). */
    private UUID clienteRef;
    private boolean activo;
    private LocalDateTime favoritoDesde;
    private LocalDateTime removidoAt;
}