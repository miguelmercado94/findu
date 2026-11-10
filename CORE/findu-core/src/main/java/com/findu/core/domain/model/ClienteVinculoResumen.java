package com.findu.core.domain.model;

import java.util.UUID;

/** Vista de lectura de un cliente vinculado a un proveedor (uso interno; no es el DTO de la API). */
public record ClienteVinculoResumen(
        Long perfilClienteId,
        UUID clienteRef,
        String nombreCompleto,
        Boolean tieneFoto,
        Boolean favoritoInterno,
        Long serviciosRealizados,
        Boolean enServicio,
        Boolean activo) {
}