package com.findu.transaction.application.port.in.command;

import com.findu.transaction.domain.enums.TransferSimulationOutcome;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExecuteRefundCommand {
    private final String refundId;
    private final TransferSimulationOutcome outcome;
}
