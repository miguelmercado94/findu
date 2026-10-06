package com.findu.transaction.domain.event;

import com.findu.transaction.domain.valueobject.Money;
import lombok.Getter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Getter
public class ProviderDebtCreatedEvent implements DomainEvent {

    private final String providerDebtId;
    private final Long providerId;
    private final Money amount;
    private final String reference;
    private final Instant occurredAt;

    public ProviderDebtCreatedEvent(
            String providerDebtId,
            Long providerId,
            Money amount,
            String reference) {
        this.providerDebtId = providerDebtId;
        this.providerId = providerId;
        this.amount = amount;
        this.reference = reference;
        this.occurredAt = Instant.now();
    }

    @Override
    public Instant occurredOn() {
        return occurredAt;
    }

    public FinancialEventEnvelope toEnvelope(String correlationId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("providerDebtId", providerDebtId);
        payload.put("providerId", providerId);
        payload.put("amount", amount != null ? amount.getAmount() : null);
        payload.put("currency", amount != null ? amount.getCurrency().getCurrencyCode() : "COP");
        payload.put("reference", reference);

        return FinancialEventEnvelope.builder()
                .eventType("PROVIDER_DEBT_CREATED")
                .eventVersion(1)
                .aggregateId(providerDebtId != null ? providerDebtId : String.valueOf(providerId))
                .aggregateType("DEBT")
                .correlationId(correlationId)
                .payload(payload)
                .build();
    }
}
