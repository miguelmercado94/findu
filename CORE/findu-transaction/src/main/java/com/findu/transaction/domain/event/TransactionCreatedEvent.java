package com.findu.transaction.domain.event;

import com.findu.transaction.domain.enums.PaymentMethod;
import com.findu.transaction.domain.valueobject.Money;
import lombok.Getter;

import java.time.Instant;

@Getter
public class TransactionCreatedEvent implements DomainEvent {

    private final String transactionId;
    private final String transactionCode;
    private final Long serviceRequestId;
    private final Long providerId;
    private final Long customerId;
    private final Money totalCustomerAmount;
    private final Money providerAmount;
    private final PaymentMethod paymentMethod;
    private final Instant occurredOn;

    public TransactionCreatedEvent(
            String transactionId,
            String transactionCode,
            Long serviceRequestId,
            Long providerId,
            Long customerId,
            Money totalCustomerAmount,
            Money providerAmount,
            PaymentMethod paymentMethod) {

        this.transactionId = transactionId;
        this.transactionCode = transactionCode;
        this.serviceRequestId = serviceRequestId;
        this.providerId = providerId;
        this.customerId = customerId;
        this.totalCustomerAmount = totalCustomerAmount;
        this.providerAmount = providerAmount;
        this.paymentMethod = paymentMethod;
        this.occurredOn = Instant.now();
    }

    @Override
    public Instant occurredOn() {
        return occurredOn;
    }
}
