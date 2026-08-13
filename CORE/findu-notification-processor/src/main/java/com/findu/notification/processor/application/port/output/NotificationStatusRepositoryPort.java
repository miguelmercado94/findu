package com.findu.notification.processor.application.port.output;

import com.findu.notification.processor.domain.model.NotificationMessage;

/**
 * Puerto de salida para persistir el estado del procesamiento de notificaciones (SUCCESS/FAILED)
 * con los reintentos, el código del error y el payload original en JSON.
 */
public interface NotificationStatusRepositoryPort {
    void saveSuccess(String notificationId);
    void saveFailure(NotificationMessage message, String errorCode, String errorDesc, int retries);
}
