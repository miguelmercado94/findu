package com.findu.help.v2.infrastructure.adapter.out.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventEnvelopeDto<T> {
    private String eventType;
    private String eventId;
    private Instant timestamp;
    private String targetUserId;
    private T payload;
}
