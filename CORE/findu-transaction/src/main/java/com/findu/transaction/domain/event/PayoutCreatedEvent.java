package com.findu.transaction.domain.event;

import com.findu.transaction.domain.enums.PaymentMethod;
import com.findu.transaction.domain.valueobject.Money;
import lombok.Getter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Getter
public class PayoutCreatedEvent implements DomainEvent {

    private final String payoutId;
    private final String settlementId;
    private final Long providerId;
    private final Money amount;
    private final PaymentMethod paymentMethod;
    private final Instant scheduledAt;

    public PayoutCreatedEvent(
            String payoutId,
            String settlementId,
            Long providerId,
            Money amount,
            PaymentMethod paymentMethod,
            Instant scheduledAt) {
        this.payoutId = payoutId;
        this.settlementId = settlementId;
        this.providerId = providerId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.scheduledAt = scheduledAt != null ? scheduledAt : Instant.now();
    }

    public PayoutCreatedEvent(String payoutId, String payoutCode, Long providerId, Money amount) {
        this(payoutId, payoutCode, providerId, amount, PaymentMethod.SIMULATED, Instant.now());
    }

    @Override
    public Instant occurredOn() {
        return scheduledAt;
    }

    public FinancialEventEnvelope toEnvelope(String correlationId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("payoutId", payoutId);
        payload.put("settlementId", settlementId);
        payload.put("providerId", providerId);
        payload.put("amount", amount != null ? amount.getAmount() : null);
        payload.put("currency", amount != null ? amount.getCurrency().getCurrencyCode() : "COP");
        payload.put("paymentMethod", paymentMethod != null ? paymentMethod.name() : null);
        payload.put("scheduledAt", scheduledAt.toString());

        return FinancialEventEnvelope.builder()
                .eventType("PAYOUT_CREATED")
                .eventVersion(1)
                .aggregateId(payoutId)
                .aggregateType("PAYOUT")
                .correlationId(correlationId)
                .payload(payload)
                .build();
    }
}
