package com.findu.transaction.application.port.in;

import com.findu.transaction.application.port.in.command.RequestExtraExpenseCommand;
import com.findu.transaction.domain.model.expense.ExtraExpenseRequest;

public interface ManageExtraExpenseUseCase {
    ExtraExpenseRequest requestExpense(RequestExtraExpenseCommand command);
    ExtraExpenseRequest approveExpense(String expenseId);
    ExtraExpenseRequest rejectExpense(String expenseId);
}
