package com.findu.transaction.application.port.in;

import com.findu.transaction.application.port.in.command.RegisterPaymentCommand;
import com.findu.transaction.domain.model.transaction.ServiceTransaction;

public interface RegisterPaymentUseCase {
    ServiceTransaction execute(RegisterPaymentCommand command);
}
