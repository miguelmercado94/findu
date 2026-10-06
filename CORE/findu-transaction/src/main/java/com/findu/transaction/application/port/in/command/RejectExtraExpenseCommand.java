package com.findu.transaction.application.port.in.command;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RejectExtraExpenseCommand {
    private final String expenseId;
    private final String rejectionReason;
    private final String rejectedBySupportAgentId;
}
