package com.findu.transaction.application.port.in;

import com.findu.transaction.application.port.in.query.GetDailyBalanceQuery;
import com.findu.transaction.domain.model.balance.DailyBalance;

import java.util.Optional;

public interface GetDailyBalanceUseCase {
    Optional<DailyBalance> execute(GetDailyBalanceQuery query);
}
