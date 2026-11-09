package com.findu.core.application.util;

import com.findu.core.domain.exception.ArchivoProveedorException;
import com.findu.core.domain.exception.ArchivoProveedorException.Codigo;

import java.util.Base64;

/** Decodificacion de imagenes recibidas como base64 puro o data-URI. */
public final class Base64Images {

    private Base64Images() {
    }

    /** @throws ArchivoProveedorException IMAGEN_INVALIDA (422) si esta vacio o no es base64 valido */
    public static byte[] decode(String value) {
        if (value == null || value.isBlank()) {
            throw new ArchivoProveedorException(Codigo.IMAGEN_INVALIDA, "La imagen es requerida");
        }
        String payload = value.trim();
        int comma = payload.indexOf(',');
        if (payload.regionMatches(true, 0, "data:", 0, 5) && comma >= 0) {
            payload = payload.substring(comma + 1);
        }
        try {
            return Base64.getMimeDecoder().decode(payload);
        } catch (IllegalArgumentException e) {
            throw new ArchivoProveedorException(Codigo.IMAGEN_INVALIDA, "La imagen no es un base64 valido");
        }
    }
}
