package com.findu.transaction.application.port.in;

import com.findu.transaction.application.port.in.command.RequestExtraExpenseCommand;
import com.findu.transaction.domain.model.expense.ExtraExpenseRequest;

public interface RequestExtraExpenseUseCase {
    ExtraExpenseRequest execute(RequestExtraExpenseCommand command);
}
