package com.findu.transaction.domain.event;

import com.findu.transaction.domain.valueobject.Money;
import lombok.Getter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Getter
public class PayoutFailedEvent implements DomainEvent {

    private final String payoutId;
    private final String settlementId;
    private final Long providerId;
    private final Money amount;
    private final String reason;
    private final Instant occurredAt;

    public PayoutFailedEvent(
            String payoutId,
            String settlementId,
            Long providerId,
            Money amount,
            String reason) {
        this.payoutId = payoutId;
        this.settlementId = settlementId;
        this.providerId = providerId;
        this.amount = amount;
        this.reason = reason;
        this.occurredAt = Instant.now();
    }

    @Override
    public Instant occurredOn() {
        return occurredAt;
    }

    public FinancialEventEnvelope toEnvelope(String correlationId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("payoutId", payoutId);
        payload.put("settlementId", settlementId);
        payload.put("providerId", providerId);
        payload.put("amount", amount != null ? amount.getAmount() : null);
        payload.put("currency", amount != null ? amount.getCurrency().getCurrencyCode() : "COP");
        payload.put("reason", reason);

        return FinancialEventEnvelope.builder()
                .eventType("PAYOUT_FAILED")
                .eventVersion(1)
                .aggregateId(payoutId)
                .aggregateType("PAYOUT")
                .correlationId(correlationId)
                .payload(payload)
                .build();
    }
}
