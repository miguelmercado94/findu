package com.findu.transaction.application.port.in;

import com.findu.transaction.application.port.in.command.CreateRefundCommand;
import com.findu.transaction.domain.model.refund.Refund;

public interface CreateRefundUseCase {
    Refund execute(CreateRefundCommand command);
}
