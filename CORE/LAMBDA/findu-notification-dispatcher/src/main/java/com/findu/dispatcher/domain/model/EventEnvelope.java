package com.findu.dispatcher.domain.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

/**
 * Envelope Pattern genérico para multiplexación de eventos sobre un único canal WebSocket.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EventEnvelope<T>(
        EventType eventType,
        String eventId,
        Instant timestamp,
        String targetUserId,
        T payload
) {
    public static <T> EventEnvelope<T> of(EventType eventType, String targetUserId, T payload) {
        return new EventEnvelope<>(
                eventType,
                UUID.randomUUID().toString(),
                Instant.now(),
                targetUserId,
                payload
        );
    }

    public static EventEnvelope<String> ping() {
        return new EventEnvelope<>(
                EventType.PING,
                UUID.randomUUID().toString(),
                Instant.now(),
                null,
                "ping"
        );
    }
}
