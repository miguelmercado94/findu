package com.findu.transaction.application.port.in;

import com.findu.transaction.application.port.in.command.CreatePayoutCommand;
import com.findu.transaction.domain.model.payout.Payout;

public interface CreatePayoutUseCase {
    Payout execute(CreatePayoutCommand command);
}
