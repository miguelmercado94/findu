package com.findu.transaction.domain.event;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
public class FinancialEventEnvelope implements DomainEvent {

    @Builder.Default
    private final String eventId = UUID.randomUUID().toString();
    private final String eventType;
    @Builder.Default
    private final int eventVersion = 1;
    @Builder.Default
    private final Instant occurredAt = Instant.now();
    private final String aggregateId;
    private final String aggregateType;
    private final String correlationId;
    private final String causationId;
    @Builder.Default
    private final String producer = "findu-transaction";
    private final Map<String, Object> payload;

    @Override
    public Instant occurredOn() {
        return occurredAt;
    }
}
