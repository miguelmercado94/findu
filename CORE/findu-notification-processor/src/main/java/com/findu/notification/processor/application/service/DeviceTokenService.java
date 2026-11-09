package com.findu.notification.processor.application.service;

import com.findu.notification.processor.application.port.out.externalapi.SecurityUserLookupPort;
import com.findu.notification.processor.application.port.out.persistence.DeviceTokenRepositoryPort;
import com.findu.notification.processor.domain.model.DeviceToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

/**
 * Servicio de registro/baja de device tokens.
 *
 * En el registro resuelve (best-effort) el recipient canónico
 * {@code PROVEEDOR_<proveedorId>} vía {@link SecurityUserLookupPort} y persiste el token
 * con username + canonical_recipient (null si security no resuelve el proveedor).
 * Nunca lanza al controller: los errores se registran y se tragan.
 */
@Service
public class DeviceTokenService {

    private static final Logger log = LoggerFactory.getLogger(DeviceTokenService.class);
    private static final String CANONICAL_PREFIX = "PROVEEDOR_";

    private final DeviceTokenRepositoryPort deviceTokenRepository;
    private final SecurityUserLookupPort securityUserLookupPort;

    public DeviceTokenService(DeviceTokenRepositoryPort deviceTokenRepository,
                              SecurityUserLookupPort securityUserLookupPort) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.securityUserLookupPort = securityUserLookupPort;
    }

    public void registerToken(String username, String deviceToken, String platform) {
        try {
            String canonicalRecipient = resolveCanonicalRecipient(username);
            Instant now = Instant.now();
            DeviceToken token = DeviceToken.builder()
                    .username(username)
                    .deviceToken(deviceToken)
                    .platform(platform)
                    .canonicalRecipient(canonicalRecipient)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            deviceTokenRepository.upsert(token);
        } catch (Exception e) {
            log.error("Error registrando device token (best-effort): {}", e.getMessage(), e);
        }
    }

    public void unregister(String username, String deviceToken) {
        try {
            deviceTokenRepository.delete(username, deviceToken);
        } catch (Exception e) {
            log.error("Error desregistrando device token (best-effort): {}", e.getMessage(), e);
        }
    }

    private String resolveCanonicalRecipient(String username) {
        Optional<Long> proveedorId = securityUserLookupPort.findProveedorIdByUsername(username);
        return proveedorId.map(id -> CANONICAL_PREFIX + id).orElse(null);
    }
}
