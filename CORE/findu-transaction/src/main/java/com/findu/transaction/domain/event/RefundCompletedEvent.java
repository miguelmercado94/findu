package com.findu.transaction.domain.event;

import com.findu.transaction.domain.valueobject.Money;
import lombok.Getter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Getter
public class RefundCompletedEvent implements DomainEvent {

    private final String refundId;
    private final String transactionId;
    private final Long customerId;
    private final Money amount;
    private final String reference;
    private final Instant processedAt;

    public RefundCompletedEvent(
            String refundId,
            String transactionId,
            Long customerId,
            Money amount,
            String reference,
            Instant processedAt) {
        this.refundId = refundId;
        this.transactionId = transactionId;
        this.customerId = customerId;
        this.amount = amount;
        this.reference = reference;
        this.processedAt = processedAt != null ? processedAt : Instant.now();
    }

    public RefundCompletedEvent(String refundId, String refundCode, Long customerId, Money amount, String externalReference) {
        this(refundId, refundCode, customerId, amount, externalReference, Instant.now());
    }

    @Override
    public Instant occurredOn() {
        return processedAt;
    }

    public FinancialEventEnvelope toEnvelope(String correlationId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("refundId", refundId);
        payload.put("transactionId", transactionId);
        payload.put("customerId", customerId);
        payload.put("amount", amount != null ? amount.getAmount() : null);
        payload.put("currency", amount != null ? amount.getCurrency().getCurrencyCode() : "COP");
        payload.put("reference", reference);
        payload.put("processedAt", processedAt.toString());

        return FinancialEventEnvelope.builder()
                .eventType("REFUND_COMPLETED")
                .eventVersion(1)
                .aggregateId(refundId)
                .aggregateType("REFUND")
                .correlationId(correlationId)
                .payload(payload)
                .build();
    }
}
