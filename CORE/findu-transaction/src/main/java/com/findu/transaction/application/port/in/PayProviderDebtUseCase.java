package com.findu.transaction.application.port.in;

import com.findu.transaction.application.port.in.command.PayProviderDebtCommand;
import com.findu.transaction.domain.model.debt.ProviderDebtPayment;

public interface PayProviderDebtUseCase {
    ProviderDebtPayment execute(PayProviderDebtCommand command);
}
