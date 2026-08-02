package com.findu.core.application.port.output.externalapi;

/**
 * Puerto de salida para comunicación con el microservicio findu-s3-servicios.
 * Gestiona subida y consulta de archivos (imágenes de perfil, portafolio, etc.)
 *
 * Contrato del micro findu-s3-servicios:
 * - POST /api/v1/files → sube archivo (bucket, fileName, fileBase64) → retorna URL
 * - GET  /api/v1/files?url={url} → retorna el fileBase64 del archivo almacenado
 */
public interface StorageServicePort {

    /**
     * Sube un archivo al servicio de almacenamiento.
     *
     * @param bucket     nombre del bucket (ej: "perfiles", "portafolio")
     * @param fileName   nombre del archivo (ej: username del usuario)
     * @param fileBase64 contenido del archivo codificado en Base64
     * @return URL pública/interna del archivo almacenado, o null si el servicio no está disponible
     */
    String uploadFile(String bucket, String fileName, String fileBase64);

    /**
     * Obtiene el contenido Base64 de un archivo a partir de su URL.
     *
     * @param fileUrl URL del archivo almacenado
     * @return contenido del archivo en Base64, o null si no se encuentra
     */
    String getFileBase64(String fileUrl);
}
