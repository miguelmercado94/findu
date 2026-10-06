package com.findu.transaction.application.port.in;

import com.findu.transaction.application.port.in.command.ExecuteRefundCommand;
import com.findu.transaction.domain.model.refund.Refund;

public interface ExecuteRefundUseCase {
    Refund execute(ExecuteRefundCommand command);
}
