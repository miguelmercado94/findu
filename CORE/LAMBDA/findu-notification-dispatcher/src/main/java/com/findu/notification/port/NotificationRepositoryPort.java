package com.findu.notification.port;

import com.findu.notification.model.NotificationRecord;

/**
 * Puerto de persistencia para registros de notificación.
 * La capa de servicio depende de esta interface, NO de DynamoDB directamente.
 * Puede ser implementado con DynamoDB, MongoDB, PostgreSQL, etc.
 */
public interface NotificationRepositoryPort {

    /**
     * Guarda un registro de notificación.
     */
    void save(NotificationRecord record);
}
