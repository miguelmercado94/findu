package com.findu.transaction.domain.event;

import lombok.Getter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Getter
public class ExtraExpenseRejectedEvent implements DomainEvent {

    private final String extraExpenseRequestId;
    private final String transactionId;
    private final Long providerId;
    private final String reason;
    private final Instant rejectedAt;

    public ExtraExpenseRejectedEvent(
            String extraExpenseRequestId,
            String transactionId,
            Long providerId,
            String reason,
            Instant rejectedAt) {
        this.extraExpenseRequestId = extraExpenseRequestId;
        this.transactionId = transactionId;
        this.providerId = providerId;
        this.reason = reason;
        this.rejectedAt = rejectedAt != null ? rejectedAt : Instant.now();
    }

    @Override
    public Instant occurredOn() {
        return rejectedAt;
    }

    public FinancialEventEnvelope toEnvelope(String correlationId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("extraExpenseRequestId", extraExpenseRequestId);
        payload.put("transactionId", transactionId);
        payload.put("providerId", providerId);
        payload.put("reason", reason);
        payload.put("rejectedAt", rejectedAt.toString());

        return FinancialEventEnvelope.builder()
                .eventType("EXTRA_EXPENSE_REJECTED")
                .eventVersion(1)
                .aggregateId(extraExpenseRequestId)
                .aggregateType("EXTRA_EXPENSE")
                .correlationId(correlationId)
                .payload(payload)
                .build();
    }
}
