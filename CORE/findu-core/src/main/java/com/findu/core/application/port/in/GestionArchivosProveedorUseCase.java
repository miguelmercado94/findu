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

    /**
     * Registra una validacion biometrica (primera o nueva): normaliza a PNG y escribe biometrico/rostro/rostro.png,
     * biometrico/documento/frente.png y reverso.png; luego copia el rostro a perfil/ y marca el espejo
     * biometria_verificada_at y estado_verificacion=VERIFICADO. Idempotente.
     */
    BiometriaResultado registrarBiometria(Long perfilId, BiometriaCommand command);

    /** Imagenes en base64 o data-URI. Rostro obligatorio; documentos opcionales. */
    record BiometriaCommand(String rostroBase64, String documentoFrenteBase64, String documentoReversoBase64,
                            Double faceMatchScore) {
    }

    record BiometriaResultado(Long perfilId, boolean fotoPerfilActualizada) {
    }

    record FotoPerfil(byte[] bytes, String contentType, String key) {
    }
}
