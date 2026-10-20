package com.findu.transaction.application.port.in.finance.command;

import com.findu.transaction.domain.valueobject.Money;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateBreBOrderCommand {
    private final Long providerId;
    private final Money amount;
    private final String username;
}
