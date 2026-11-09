package com.findu.notification.processor.infrastructure.adapter.out.externalapi;

import com.findu.notification.processor.application.port.out.externalapi.NotificationSenderPort;
import com.findu.notification.processor.application.port.out.persistence.DeviceTokenRepositoryPort;
import com.findu.notification.processor.domain.enums.NotificationChannel;
import com.findu.notification.processor.domain.model.NotificationResult;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Sender PUSH real vía Firebase Cloud Messaging (FCM).
 *
 * <p>Resuelve los device tokens por recipient canónico (PROVEEDOR_&lt;proveedorId&gt;) desde el
 * {@link DeviceTokenRepositoryPort} y envía un {@link Message} por token con notification(title/body)
 * y data (type=TICKET_CHAT, ticketCode, senderName, mensaje) que la app usa para navegar al chat.</p>
 *
 * <p>Degradable: si {@link FirebaseMessaging} no está disponible (sin credencial) o no hay tokens,
 * hace no-op con success=true. Un token UNREGISTERED/INVALID_ARGUMENT se elimina del store. NUNCA
 * propaga excepciones que rompan el consumer y NUNCA loguea el token completo.</p>
 */
@Component
public class FirebasePushSender implements NotificationSenderPort {

    private static final Logger log = LoggerFactory.getLogger(FirebasePushSender.class);

    private final FirebaseMessaging messaging;
    private final DeviceTokenRepositoryPort deviceTokenRepository;

    public FirebasePushSender(@Autowired(required = false) FirebaseMessaging messaging,
                              DeviceTokenRepositoryPort deviceTokenRepository) {
        this.messaging = messaging;
        this.deviceTokenRepository = deviceTokenRepository;
    }

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.PUSH;
    }

    @Override
    public NotificationResult send(String recipient, String subject, String body, Map<String, String> metadata) {
        if (messaging == null) {
            log.warn("[FCM] Firebase deshabilitado (sin credencial). Push a={} tratado como no-op.", recipient);
            return success(recipient);
        }

        List<String> tokens = deviceTokenRepository.findTokensByCanonicalRecipient(recipient);
        if (tokens == null || tokens.isEmpty()) {
            log.info("[FCM] Sin device tokens para recipient={}. No-op.", recipient);
            return success(recipient);
        }

        int sent = 0;
        int nonTokenFailures = 0;
        for (String token : tokens) {
            try {
                Message message = buildMessage(token, subject, body, metadata);
                messaging.send(message);
                sent++;
                log.info("[FCM] Push enviado a recipient={} token={}", recipient, mask(token));
            } catch (FirebaseMessagingException e) {
                MessagingErrorCode code = e.getMessagingErrorCode();
                if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
                    log.info("[FCM] Token inválido ({}), se elimina del store. token={}", code, mask(token));
                    safeDeleteToken(token);
                } else {
                    nonTokenFailures++;
                    log.error("[FCM] Error al enviar push a token={} code={} msg={}", mask(token), code, e.getMessage());
                }
            } catch (Exception e) {
                nonTokenFailures++;
                log.error("[FCM] Error inesperado al enviar push a token={}: {}", mask(token), e.getMessage());
            }
        }

        // success=true si al menos un envío fue OK o no había fallos no-token;
        // success=false solo si TODOS fallaron por error no-token (para activar la política de reintento).
        boolean success = sent > 0 || nonTokenFailures == 0;
        if (success) {
            return success(recipient);
        }
        return NotificationResult.builder()
                .success(false)
                .channel(NotificationChannel.PUSH)
                .recipient(recipient)
                .error("Todos los envíos FCM fallaron (" + nonTokenFailures + " errores no-token)")
                .build();
    }

    private Message buildMessage(String token, String subject, String body, Map<String, String> metadata) {
        Message.Builder builder = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(subject)
                        .setBody(body)
                        .build());

        Map<String, String> meta = metadata != null ? metadata : Map.of();
        builder.putData("type", meta.getOrDefault("type", "TICKET_CHAT"));
        putIfPresent(builder, "ticketCode", meta.get("ticketCode"));
        putIfPresent(builder, "senderName", meta.get("senderName"));
        putIfPresent(builder, "mensaje", meta.get("mensaje"));
        return builder.build();
    }

    private void putIfPresent(Message.Builder builder, String key, String value) {
        if (value != null) {
            builder.putData(key, value);
        }
    }

    private void safeDeleteToken(String token) {
        try {
            deviceTokenRepository.deleteByToken(token);
        } catch (Exception e) {
            log.error("[FCM] No se pudo eliminar el token inválido token={}: {}", mask(token), e.getMessage());
        }
    }

    private NotificationResult success(String recipient) {
        return NotificationResult.builder()
                .success(true)
                .channel(NotificationChannel.PUSH)
                .recipient(recipient)
                .build();
    }

    /** Trunca el token para no filtrarlo completo en logs. */
    private String mask(String token) {
        if (token == null || token.length() <= 8) {
            return "***";
        }
        return token.substring(0, 6) + "..." + token.substring(token.length() - 4);
    }
}
