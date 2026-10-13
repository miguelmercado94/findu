package com.findu.notification.processor.application.port.out.persistence;

import com.findu.notification.processor.domain.enums.TemplateType;
import com.findu.notification.processor.domain.model.NotificationTemplate;

import java.util.List;
import java.util.Optional;

/**
 * Puerto para acceder a las plantillas de notificación.
 * No depende de DynamoDB — puede ser cualquier BD no relacional.
 *
 * <p>El esquema de persistencia es versionado (diseño §9): PK {@code id = CODE#TIPO#LANG}
 * + SK {@code v#<n>} (versiones inmutables) y {@code v#ACTIVE} (puntero a la versión
 * activa, desnormalizado para resolver el envío en una sola lectura).</p>
 */
public interface TemplateRepositoryPort {

    // ---- Hot path de envío (resolución de versión activa) ----

    /**
     * Resuelve la versión activa (SK {@code v#ACTIVE}) para el código/tipo/idioma dados.
     * Es la ruta de lectura usada por el flujo de envío.
     */
    Optional<NotificationTemplate> findActive(String codigo, TemplateType tipo, String language);

    // ---- Operaciones de administración (versionado §9) ----

    /**
     * Crea una versión nueva: escribe el ítem inmutable {@code v#<n+1>} y actualiza
     * el puntero {@code v#ACTIVE} en una sola transacción (TransactWriteItems).
     */
    void createNewVersion(NotificationTemplate tpl);

    /**
     * Rollback / activación: repunta {@code v#ACTIVE} a una versión {@code v#<version>} previa.
     */
    void activateVersion(String id, int version);

    /**
     * Lista todas las versiones (SK begins_with {@code v#}) de una plantilla (histórico).
     */
    List<NotificationTemplate> findHistory(String id);

    /**
     * Lista las plantillas (versión activa) de un tipo funcional (GSI por tipo).
     */
    List<NotificationTemplate> findByTipo(TemplateType tipo);

    /**
     * Desactiva la versión activa de una plantilla ({@code is_active = false} en {@code v#ACTIVE}).
     */
    void deactivate(String id);

    // ---- Compatibilidad con el flujo de envío legacy ----

    /**
     * Busca una plantilla por código, canal e idioma.
     * Resuelve mapeando el canal técnico a {@link TemplateType} y leyendo la versión activa.
     */
    Optional<NotificationTemplate> findByCodeAndChannelAndLanguage(String templateCode, String channel, String language);

    /**
     * Fallback: busca por código y canal con idioma por defecto (es).
     */
    Optional<NotificationTemplate> findByCodeAndChannel(String templateCode, String channel);
}
