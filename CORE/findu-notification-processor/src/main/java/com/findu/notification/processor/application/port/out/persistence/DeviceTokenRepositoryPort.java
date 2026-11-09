package com.findu.notification.processor.application.port.out.persistence;

import com.findu.notification.processor.domain.model.DeviceToken;

import java.util.List;

/**
 * Puerto de salida para persistir y consultar los device tokens de Firebase.
 * El {@code deviceToken} es la PK; el upsert es idempotente por token.
 */
public interface DeviceTokenRepositoryPort {

    /** Inserta o reemplaza el token (idempotente por deviceToken como PK). */
    void upsert(DeviceToken deviceToken);

    /** Lista los device tokens asociados a un recipient canónico (PROVEEDOR_&lt;proveedorId&gt;). */
    List<String> findTokensByCanonicalRecipient(String canonicalRecipient);

    /** Lista los device tokens registrados por un username. */
    List<String> findTokensByUsername(String username);

    /** Elimina el token registrado por el par username + deviceToken. */
    void delete(String username, String deviceToken);

    /** Elimina el token por su valor (PK). */
    void deleteByToken(String deviceToken);
}
