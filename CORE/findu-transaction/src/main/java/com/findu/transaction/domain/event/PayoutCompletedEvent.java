package com.findu.transaction.domain.event;

import com.findu.transaction.domain.valueobject.Money;
import lombok.Getter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Getter
public class PayoutCompletedEvent implements DomainEvent {

    private final String payoutId;
    private final String settlementId;
    private final Long providerId;
    private final Money amount;
    private final String reference;
    private final Instant executedAt;

    public PayoutCompletedEvent(
            String payoutId,
            String settlementId,
            Long providerId,
            Money amount,
            String reference,
            Instant executedAt) {
        this.payoutId = payoutId;
        this.settlementId = settlementId;
        this.providerId = providerId;
        this.amount = amount;
        this.reference = reference;
        this.executedAt = executedAt != null ? executedAt : Instant.now();
    }

    public PayoutCompletedEvent(String payoutId, String payoutCode, Long providerId, Money amount, String externalReference) {
        this(payoutId, payoutCode, providerId, amount, externalReference, Instant.now());
    }

    @Override
    public Instant occurredOn() {
        return executedAt;
    }

    public FinancialEventEnvelope toEnvelope(String correlationId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("payoutId", payoutId);
        payload.put("settlementId", settlementId);
        payload.put("providerId", providerId);
        payload.put("amount", amount != null ? amount.getAmount() : null);
        payload.put("currency", amount != null ? amount.getCurrency().getCurrencyCode() : "COP");
        payload.put("reference", reference);
        payload.put("executedAt", executedAt.toString());

        return FinancialEventEnvelope.builder()
                .eventType("PAYOUT_COMPLETED")
                .eventVersion(1)
                .aggregateId(payoutId)
                .aggregateType("PAYOUT")
                .correlationId(correlationId)
                .payload(payload)
                .build();
    }
}
