package com.findu.security.application.port.output;

import reactor.core.publisher.Mono;
import java.util.Map;

/**
 * Puerto de salida para enviar notificaciones via la Lambda dispatcher.
 */
public interface NotificationPort {

    /**
     * Envía una notificación de forma asíncrona (fire-and-forget).
     * @param type canal: EMAIL, SMS, PUSH, WHATSAPP
     * @param recipient destinatario (email, teléfono con código país, o username)
     * @param templateCode código de plantilla
     * @param language idioma (es, en)
     * @param params parámetros para la plantilla
     */
    Mono<Void> send(String type, String recipient, String templateCode, String language, Map<String, String> params);
}
