package com.findu.transaction.domain.event;

import com.findu.transaction.domain.valueobject.Money;
import lombok.Getter;

import java.time.Instant;

@Getter
public class RefundRequestedEvent implements DomainEvent {

    private final String refundId;
    private final String refundCode;
    private final String transactionId;
    private final Long customerId;
    private final Money amount;
    private final Instant occurredOn;

    public RefundRequestedEvent(String refundId, String refundCode, String transactionId, Long customerId, Money amount) {
        this.refundId = refundId;
        this.refundCode = refundCode;
        this.transactionId = transactionId;
        this.customerId = customerId;
        this.amount = amount;
        this.occurredOn = Instant.now();
    }

    @Override
    public Instant occurredOn() {
        return occurredOn;
    }
}
