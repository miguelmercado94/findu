package com.findu.transaction.application.port.in.command;

import com.findu.transaction.domain.valueobject.Money;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreatePayoutCommand {
    private final Long providerId;
    private final String settlementId;
    private final Money amount;
}
