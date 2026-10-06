package com.findu.notification.processor.dto.financial;

import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
public class FinancialEventEnvelopeDto {
    private String eventId;
    private String eventType;
    private int eventVersion;
    private Instant occurredAt;
    private String aggregateId;
    private String aggregateType;
    private String correlationId;
    private String causationId;
    private String producer;
    private Map<String, Object> payload;
}
