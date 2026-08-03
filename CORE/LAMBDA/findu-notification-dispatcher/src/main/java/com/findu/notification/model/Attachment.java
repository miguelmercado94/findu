package com.findu.notification.model;

import lombok.*;

/**
 * Anexo de una notificación (solo aplica para correos).
 * Se envía la URL de S3, no el archivo binario.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attachment {

    /** URL del archivo en S3 (bucket de anexos) */
    private String url;

    /** Nombre del archivo (ej: "factura_98765.pdf") */
    private String fileName;

    /** Tipo MIME (ej: "application/pdf", "image/png") */
    private String contentType;
}
