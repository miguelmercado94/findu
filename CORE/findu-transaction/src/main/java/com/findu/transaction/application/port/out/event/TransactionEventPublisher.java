package com.findu.transaction.application.port.out.event;

import com.findu.transaction.domain.event.FinancialEventEnvelope;

public interface TransactionEventPublisher {
    void publish(FinancialEventEnvelope eventEnvelope);
}
