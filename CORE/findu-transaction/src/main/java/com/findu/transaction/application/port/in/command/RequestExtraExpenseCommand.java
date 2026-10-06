package com.findu.transaction.application.port.in.command;

import com.findu.transaction.domain.valueobject.Money;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RequestExtraExpenseCommand {
    private final Long serviceRequestId;
    private final Long providerId;
    private final Money amount;
    private final String reason;
}
