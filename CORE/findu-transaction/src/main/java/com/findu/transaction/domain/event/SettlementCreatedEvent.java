package com.findu.transaction.domain.event;

import com.findu.transaction.domain.valueobject.Money;
import lombok.Getter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Getter
public class SettlementCreatedEvent implements DomainEvent {

    private final String settlementId;
    private final Long providerId;
    private final Instant periodStart;
    private final Instant periodEnd;
    private final Money amount;
    private final Instant occurredOn;

    public SettlementCreatedEvent(String settlementId, Long providerId, Instant periodStart, Instant periodEnd, Money amount) {
        this.settlementId = settlementId;
        this.providerId = providerId;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.amount = amount;
        this.occurredOn = Instant.now();
    }

    @Override
    public Instant occurredOn() {
        return occurredOn;
    }

    public FinancialEventEnvelope toEnvelope(String correlationId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("settlementId", settlementId);
        payload.put("providerId", providerId);
        payload.put("amount", amount != null ? amount.getAmount() : null);
        payload.put("currency", amount != null ? amount.getCurrency().getCurrencyCode() : "COP");
        payload.put("periodStart", periodStart != null ? periodStart.toString() : null);
        payload.put("periodEnd", periodEnd != null ? periodEnd.toString() : null);

        return FinancialEventEnvelope.builder()
                .eventType("SETTLEMENT_CREATED")
                .eventVersion(1)
                .aggregateId(settlementId)
                .aggregateType("SETTLEMENT")
                .correlationId(correlationId)
                .payload(payload)
                .build();
    }
}
