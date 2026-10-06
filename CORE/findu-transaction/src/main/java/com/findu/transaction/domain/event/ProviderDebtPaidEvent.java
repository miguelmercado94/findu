package com.findu.transaction.domain.event;

import com.findu.transaction.domain.enums.PaymentMethod;
import com.findu.transaction.domain.valueobject.Money;
import lombok.Getter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Getter
public class ProviderDebtPaidEvent implements DomainEvent {

    private final String providerDebtId;
    private final Long providerId;
    private final Money amount;
    private final PaymentMethod paymentMethod;
    private final String reference;
    private final Instant paidAt;

    public ProviderDebtPaidEvent(
            String providerDebtId,
            Long providerId,
            Money amount,
            PaymentMethod paymentMethod,
            String reference,
            Instant paidAt) {
        this.providerDebtId = providerDebtId;
        this.providerId = providerId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.reference = reference;
        this.paidAt = paidAt != null ? paidAt : Instant.now();
    }

    public ProviderDebtPaidEvent(String debtPaymentId, Long providerId, Money amountPaid, String paymentMethodStr) {
        this(debtPaymentId, providerId, amountPaid, PaymentMethod.SIMULATED, paymentMethodStr, Instant.now());
    }

    @Override
    public Instant occurredOn() {
        return paidAt;
    }

    public FinancialEventEnvelope toEnvelope(String correlationId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("providerDebtId", providerDebtId);
        payload.put("providerId", providerId);
        payload.put("amount", amount != null ? amount.getAmount() : null);
        payload.put("currency", amount != null ? amount.getCurrency().getCurrencyCode() : "COP");
        payload.put("paymentMethod", paymentMethod != null ? paymentMethod.name() : null);
        payload.put("reference", reference);
        payload.put("paidAt", paidAt.toString());

        return FinancialEventEnvelope.builder()
                .eventType("PROVIDER_DEBT_PAID")
                .eventVersion(1)
                .aggregateId(providerDebtId != null ? providerDebtId : String.valueOf(providerId))
                .aggregateType("DEBT")
                .correlationId(correlationId)
                .payload(payload)
                .build();
    }
}
