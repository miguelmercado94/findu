package com.findu.transaction.application.port.out.transfer;

import com.findu.transaction.domain.enums.TransferSimulationOutcome;
import com.findu.transaction.domain.valueobject.Money;

public interface TransferPort {
    TransferResult transfer(String referenceId, Long recipientId, Money amount, TransferSimulationOutcome desiredOutcome);

    record TransferResult(
            boolean isSuccessful,
            String externalTransactionId,
            String status,
            String message
    ) {}
}
