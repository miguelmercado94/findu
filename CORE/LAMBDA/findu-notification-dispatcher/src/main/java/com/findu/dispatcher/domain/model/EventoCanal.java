package com.findu.dispatcher.domain.model;

import java.time.Instant;
import java.util.UUID;

public record EventoCanal(
        String eventoId,
        Instant fechaHora,
        TipoEventoCanal tipo,
        String emisorId,
        Object contenido
) {
    public static EventoCanal of(TipoEventoCanal tipo, String emisorId, Object contenido) {
        return new EventoCanal(
                UUID.randomUUID().toString(),
                Instant.now(),
                tipo,
                emisorId,
                contenido
        );
    }
}
