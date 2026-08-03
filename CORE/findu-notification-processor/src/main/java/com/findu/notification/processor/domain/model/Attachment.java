package com.findu.notification.processor.domain.model;

import lombok.*;

/**
 * Anexo de una notificación de correo.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attachment {
    private String url;         // URL S3
    private String fileName;   // ej: "factura_98765.pdf"
    private String contentType; // ej: "application/pdf"
}
