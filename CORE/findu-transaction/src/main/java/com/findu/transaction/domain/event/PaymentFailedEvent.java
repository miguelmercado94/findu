package com.findu.transaction.domain.event;

import com.findu.transaction.domain.enums.PaymentMethod;
import com.findu.transaction.domain.valueobject.Money;
import lombok.Getter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Getter
public class PaymentFailedEvent implements DomainEvent {

    private final String paymentId;
    private final String transactionId;
    private final Long customerId;
    private final Long providerId;
    private final Money amount;
    private final PaymentMethod paymentMethod;
    private final String reference;
    private final String reason;
    private final Instant occurredAt;

    public PaymentFailedEvent(
            String paymentId,
            String transactionId,
            Long customerId,
            Long providerId,
            Money amount,
            PaymentMethod paymentMethod,
            String reference,
            String reason) {
        this.paymentId = paymentId;
        this.transactionId = transactionId;
        this.customerId = customerId;
        this.providerId = providerId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.reference = reference;
        this.reason = reason;
        this.occurredAt = Instant.now();
    }

    @Override
    public Instant occurredOn() {
        return occurredAt;
    }

    public FinancialEventEnvelope toEnvelope(String correlationId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("paymentId", paymentId);
        payload.put("transactionId", transactionId);
        payload.put("customerId", customerId);
        payload.put("providerId", providerId);
        payload.put("amount", amount != null ? amount.getAmount() : null);
        payload.put("currency", amount != null ? amount.getCurrency().getCurrencyCode() : "COP");
        payload.put("paymentMethod", paymentMethod != null ? paymentMethod.name() : null);
        payload.put("reference", reference);
        payload.put("reason", reason);

        return FinancialEventEnvelope.builder()
                .eventType("PAYMENT_FAILED")
                .eventVersion(1)
                .aggregateId(paymentId != null ? paymentId : transactionId)
                .aggregateType("PAYMENT")
                .correlationId(correlationId)
                .payload(payload)
                .build();
    }
}
