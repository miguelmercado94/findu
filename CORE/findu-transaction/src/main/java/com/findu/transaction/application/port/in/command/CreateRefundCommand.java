package com.findu.transaction.application.port.in.command;

import com.findu.transaction.domain.valueobject.Money;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateRefundCommand {
    private final String transactionId;
    private final Long customerId;
    private final Money amount;
    private final String reason;
}
