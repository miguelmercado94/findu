package com.findu.transaction.application.port.in.finance;

import com.findu.transaction.application.port.in.finance.command.ConfirmBankConsignmentCommand;
import com.findu.transaction.domain.model.finance.BankConsignmentReport;

public interface ConfirmBankConsignmentUseCase {
    BankConsignmentReport confirmConsignment(ConfirmBankConsignmentCommand command);
}
