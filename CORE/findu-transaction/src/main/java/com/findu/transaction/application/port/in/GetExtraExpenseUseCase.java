package com.findu.transaction.application.port.in;

import com.findu.transaction.application.port.in.query.GetExtraExpenseQuery;
import com.findu.transaction.domain.model.expense.ExtraExpenseRequest;

import java.util.Optional;

public interface GetExtraExpenseUseCase {
    Optional<ExtraExpenseRequest> execute(GetExtraExpenseQuery query);
}
