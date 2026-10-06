package com.findu.transaction.domain.event;

import com.findu.transaction.domain.valueobject.Money;
import lombok.Getter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Getter
public class RefundFailedEvent implements DomainEvent {

    private final String refundId;
    private final String transactionId;
    private final Long customerId;
    private final Money amount;
    private final String reason;
    private final Instant occurredAt;

    public RefundFailedEvent(
            String refundId,
            String transactionId,
            Long customerId,
            Money amount,
            String reason) {
        this.refundId = refundId;
        this.transactionId = transactionId;
        this.customerId = customerId;
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
        payload.put("refundId", refundId);
        payload.put("transactionId", transactionId);
        payload.put("customerId", customerId);
        payload.put("amount", amount != null ? amount.getAmount() : null);
        payload.put("currency", amount != null ? amount.getCurrency().getCurrencyCode() : "COP");
        payload.put("reason", reason);

        return FinancialEventEnvelope.builder()
                .eventType("REFUND_FAILED")
                .eventVersion(1)
                .aggregateId(refundId)
                .aggregateType("REFUND")
                .correlationId(correlationId)
                .payload(payload)
                .build();
    }
}
