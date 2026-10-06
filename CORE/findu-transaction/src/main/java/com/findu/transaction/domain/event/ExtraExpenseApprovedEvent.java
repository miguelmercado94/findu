package com.findu.transaction.domain.event;

import com.findu.transaction.domain.valueobject.Money;
import lombok.Getter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Getter
public class ExtraExpenseApprovedEvent implements DomainEvent {

    private final String extraExpenseRequestId;
    private final String transactionId;
    private final Long providerId;
    private final Money approvedAmount;
    private final Instant approvedAt;

    public ExtraExpenseApprovedEvent(
            String extraExpenseRequestId,
            String transactionId,
            Long providerId,
            Money approvedAmount,
            Instant approvedAt) {
        this.extraExpenseRequestId = extraExpenseRequestId;
        this.transactionId = transactionId;
        this.providerId = providerId;
        this.approvedAmount = approvedAmount;
        this.approvedAt = approvedAt != null ? approvedAt : Instant.now();
    }

    public ExtraExpenseApprovedEvent(String expenseId, Long serviceRequestId, Long providerId, Money approvedAmount) {
        this(expenseId, String.valueOf(serviceRequestId), providerId, approvedAmount, Instant.now());
    }

    @Override
    public Instant occurredOn() {
        return approvedAt;
    }

    public FinancialEventEnvelope toEnvelope(String correlationId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("extraExpenseRequestId", extraExpenseRequestId);
        payload.put("transactionId", transactionId);
        payload.put("providerId", providerId);
        payload.put("approvedAmount", approvedAmount != null ? approvedAmount.getAmount() : null);
        payload.put("currency", approvedAmount != null ? approvedAmount.getCurrency().getCurrencyCode() : "COP");
        payload.put("approvedAt", approvedAt.toString());

        return FinancialEventEnvelope.builder()
                .eventType("EXTRA_EXPENSE_APPROVED")
                .eventVersion(1)
                .aggregateId(extraExpenseRequestId)
                .aggregateType("EXTRA_EXPENSE")
                .correlationId(correlationId)
                .payload(payload)
                .build();
    }
}
