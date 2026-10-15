package com.findu.notification.processor.domain.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

/**
 * EventEnvelope para multiplexación de eventos enviados al notification-dispatcher.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EventEnvelope<T>(
        String eventType,
        String eventId,
        Instant timestamp,
        String targetUserId,
        T payload
) {
    public static <T> EventEnvelope<T> of(String eventType, String eventId, String targetUserId, T payload) {
        return new EventEnvelope<>(
                eventType,
                eventId != null && !eventId.isBlank() ? eventId : UUID.randomUUID().toString(),
                Instant.now(),
                targetUserId,
                payload
        );
    }
}
