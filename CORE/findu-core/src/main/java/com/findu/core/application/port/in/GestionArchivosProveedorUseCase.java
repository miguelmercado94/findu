package com.findu.core.application.port.in;

/**
 * Unico servicio de archivos del proveedor (foto de perfil y biometria).
 * TODO D9: anteponer ProviderOwnershipGuard (un proveedor solo accede a sus propios archivos).
 */
public interface GestionArchivosProveedorUseCase {

    /** Bytes de la foto de perfil. 404 (ResourceNotFoundException) si no hay foto ni auto-reparacion posible. */
    FotoPerfil obtenerFotoPerfil(Long perfilId);

    /**
     * Reemplaza la foto de perfil tras validar biometria completa y rostro biometrico. Nunca escribe bajo biometrico/.
     *
     * @return URL (resuelta por FotoUrlResolver) de la nueva foto
     */
    String cambiarFotoPerfil(Long perfilId, byte[] imagen);

    record FotoPerfil(byte[] bytes, String contentType, String key) {
    }
}
