package com.findu.notification.processor.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Token de dispositivo (Firebase) registrado por la app móvil para recibir push.
 * Se persiste por {@code deviceToken} (PK) con el {@code username} que lo registró y el
 * {@code canonicalRecipient} (PROVEEDOR_&lt;proveedorId&gt;) resuelto vía lookup a security,
 * para que el fallback de push consulte los tokens por recipient canónico.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceToken {
    private String username;
    private String deviceToken;
    private String platform;
    private String canonicalRecipient;
    private Instant createdAt;
    private Instant updatedAt;
}
