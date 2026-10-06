package com.findu.core.application.port.out.externalapi;

import java.util.Map;

/**
 * Puerto de salida para enviar notificaciones via la Lambda dispatcher.
 * Versión blocking (Spring MVC). Fire-and-forget usando async.
 */
public interface NotificationPort {

    void send(String type, String recipient, String templateCode, String language, Map<String, String> params);
}
