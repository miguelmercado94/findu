package com.findu.transaction.application.port.in;

import com.findu.transaction.application.port.in.command.CreateServiceTransactionCommand;
import com.findu.transaction.domain.model.transaction.ServiceTransaction;

public interface CreateServiceTransactionUseCase {
    ServiceTransaction execute(CreateServiceTransactionCommand command);
}
