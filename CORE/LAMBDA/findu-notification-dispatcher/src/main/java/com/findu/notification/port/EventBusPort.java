package com.findu.notification.port;

import com.findu.notification.model.NotificationRecord;

/**
 * Puerto para publicar notificaciones al bus de eventos.
 * La capa de servicio depende de esta interface, NO de RabbitMQ directamente.
 * Puede ser implementado con RabbitMQ, Kafka, SQS, etc.
 */
public interface EventBusPort {

    /**
     * Publica el registro de notificación al bus de eventos.
     * @param record el registro completo (con notificationId generado)
     */
    void publish(NotificationRecord record);
}
