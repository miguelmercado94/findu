package com.findu.dispatcher.presentation.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * DTO transaccional que se serializa a JSON idéntico al que deserializa la clase
 * {@code NotificationMessage} del findu-notification-processor (otro módulo).
 *
 * Campos EXACTOS (nombres JSON = nombres de campo):
 * notificationId, type, recipient, templateCode, language, params (Map&lt;String,String&gt;),
 * requiresConnection, createdAt.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record NotificationMessagePayload(
        String notificationId,
        String type,
        String recipient,
        String templateCode,
        String language,
        Map<String, String> params,
        boolean requiresConnection,
        String createdAt
) {}
