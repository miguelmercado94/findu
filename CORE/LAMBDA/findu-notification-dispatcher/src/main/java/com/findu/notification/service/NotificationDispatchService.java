package com.findu.notification.service;

import com.findu.notification.model.DispatchResult;
import com.findu.notification.model.NotificationRecord;
import com.findu.notification.model.NotificationRequest;
import com.findu.notification.port.EventBusPort;
import com.findu.notification.port.NotificationRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Servicio principal del dispatcher.
 * Depende de ports (interfaces), NO de implementaciones concretas.
 */
@Service
public class NotificationDispatchService {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatchService.class);

    private final EventBusPort eventBusPort;
    private final NotificationRepositoryPort repositoryPort;

    public NotificationDispatchService(EventBusPort eventBusPort, NotificationRepositoryPort repositoryPort) {
        this.eventBusPort = eventBusPort;
        this.repositoryPort = repositoryPort;
    }

    /**
     * Procesa una solicitud de notificación:
     * 1. Genera un ID único
     * 2. Publica al bus de eventos
     * 3. Persiste el registro (éxito o fallo)
     */
    public DispatchResult dispatch(NotificationRequest request) {
        String notificationId = UUID.randomUUID().toString();
        String now = Instant.now().toString();

        log.info("Dispatching notification: id={} type={} recipient={} template={}",
                notificationId, request.getType(), request.getRecipient(), request.getTemplateCode());

        NotificationRecord record = NotificationRecord.builder()
                .notificationId(notificationId)
                .type(request.getType())
                .recipient(request.getRecipient())
                .templateCode(request.getTemplateCode())
                .language(request.getLanguage())
                .params(request.getParams())
                .requiresConnection(request.isRequiresConnection())
                .createdAt(now)
                .build();

        try {
            // 1. Publicar al bus de eventos
            eventBusPort.publish(record);

            // 2. Persistir como despachado exitosamente
            record.setDispatched(true);
            repositoryPort.save(record);

            return DispatchResult.builder()
                    .success(true)
                    .message("Notificación despachada al bus de eventos")
                    .notificationId(notificationId)
                    .build();

        } catch (Exception e) {
            log.error("Error dispatching notification id={}: {}", notificationId, e.getMessage(), e);

            // Persistir el fallo
            record.setDispatched(false);
            record.setError(e.getMessage());
            repositoryPort.save(record);

            return DispatchResult.builder()
                    .success(false)
                    .message("Error al despachar: " + e.getMessage())
                    .notificationId(notificationId)
                    .build();
        }
    }
}
