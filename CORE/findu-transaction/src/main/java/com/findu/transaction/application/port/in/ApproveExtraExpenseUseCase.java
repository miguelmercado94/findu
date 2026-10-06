package com.findu.transaction.application.port.in;

import com.findu.transaction.application.port.in.command.ApproveExtraExpenseCommand;
import com.findu.transaction.domain.model.expense.ExtraExpenseRequest;

public interface ApproveExtraExpenseUseCase {
    ExtraExpenseRequest execute(ApproveExtraExpenseCommand command);
}
