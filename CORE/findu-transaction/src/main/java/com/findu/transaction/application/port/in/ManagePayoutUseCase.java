package com.findu.transaction.application.port.in;

import com.findu.transaction.application.port.in.command.CreatePayoutCommand;
import com.findu.transaction.domain.enums.TransferSimulationOutcome;
import com.findu.transaction.domain.model.payout.Payout;

import java.util.List;

public interface ManagePayoutUseCase {
    Payout createPayout(CreatePayoutCommand command);
    Payout executePayout(String payoutId, TransferSimulationOutcome outcome);
    List<Payout> getPendingPayouts();
}
