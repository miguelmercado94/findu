package com.findu.transaction.application.port.in.command;

import com.findu.transaction.domain.enums.PaymentMethod;
import com.findu.transaction.domain.valueobject.Money;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CreateServiceTransactionCommand {
    private final Long serviceRequestId;
    private final Long providerId;
    private final Long customerId;
    private final Money serviceAmount;
    private final BigDecimal commissionRate;
    private final Money tipAmount;
    private final Money extraAmount;
    private final PaymentMethod paymentMethod;
}
