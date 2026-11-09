package com.findu.core.application.dto.response.internal;

/** @param fotoPerfilActualizada false si el copy a perfil/ fallo (la auto-reparacion de GET lo repara) */
public record InternalBiometriaResponse(Long perfilId, boolean fotoPerfilActualizada) {
}
