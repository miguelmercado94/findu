package com.findu.transaction.application.port.in.finance.command;

import com.findu.transaction.domain.valueobject.Money;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ConfirmBankConsignmentCommand {
    private final Long providerId;
    private final Money amount;
    private final String reference;
    private final String bankName;
    private final String confirmedBy;
    private final String notes;
}
