package com.findu.core.domain.exception;

/**
 * Regla de negocio violada al gestionar archivos del proveedor (foto de perfil, biometria).
 * El {@link Codigo} se expone al cliente; el mapeo a HTTP vive en el GlobalExceptionHandler.
 */
public class ArchivoProveedorException extends RuntimeException {

    public enum Codigo {
        /** 409: el proveedor aun no completo la biometria. */
        BIOMETRIA_REQUERIDA,
        /** 422: no hay .png bajo biometrico/rostro/. */
        ROSTRO_BIOMETRICO_NO_ENCONTRADO,
        /** 422: la comparacion facial no coincide. */
        FOTO_NO_COINCIDE,
        /** 422: base64 o imagen ilegible. */
        IMAGEN_INVALIDA,
        /** 413: la imagen supera el tamano maximo. */
        ARCHIVO_DEMASIADO_GRANDE,
        /** 415: formato distinto de png/jpeg/webp. */
        FORMATO_NO_SOPORTADO
    }

    private final Codigo codigo;

    public ArchivoProveedorException(Codigo codigo, String message) {
        super(message);
        this.codigo = codigo;
    }

    public Codigo getCodigo() {
        return codigo;
    }
}
