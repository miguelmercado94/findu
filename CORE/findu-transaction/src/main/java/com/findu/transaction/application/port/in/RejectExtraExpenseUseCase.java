package com.findu.transaction.application.port.in;

import com.findu.transaction.application.port.in.command.RejectExtraExpenseCommand;
import com.findu.transaction.domain.model.expense.ExtraExpenseRequest;

public interface RejectExtraExpenseUseCase {
    ExtraExpenseRequest execute(RejectExtraExpenseCommand command);
}
