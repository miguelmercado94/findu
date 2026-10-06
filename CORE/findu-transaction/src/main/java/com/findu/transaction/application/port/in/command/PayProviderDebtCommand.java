package com.findu.transaction.application.port.in.command;

import com.findu.transaction.domain.enums.PaymentMethod;
import com.findu.transaction.domain.valueobject.Money;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PayProviderDebtCommand {
    private final Long providerId;
    private final Money amount;
    private final PaymentMethod paymentMethod;
    private final String externalReference;
}
