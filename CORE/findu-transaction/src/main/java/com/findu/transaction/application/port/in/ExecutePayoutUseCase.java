package com.findu.transaction.application.port.in;

import com.findu.transaction.application.port.in.command.ExecutePayoutCommand;
import com.findu.transaction.domain.model.payout.Payout;

public interface ExecutePayoutUseCase {
    Payout execute(ExecutePayoutCommand command);
}
