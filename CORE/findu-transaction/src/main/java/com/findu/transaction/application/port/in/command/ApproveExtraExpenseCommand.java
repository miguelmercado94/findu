package com.findu.transaction.application.port.in.command;

import com.findu.transaction.domain.valueobject.Money;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApproveExtraExpenseCommand {
    private final String expenseId;
    private final Money approvedAmount;
    private final String approvedBySupportAgentId;
    private final String notes;
}
