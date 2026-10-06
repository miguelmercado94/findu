package com.findu.transaction.application.port.in;

import com.findu.transaction.application.port.in.command.CalculateDailyBalanceCommand;
import com.findu.transaction.domain.model.balance.DailyBalance;

import java.time.LocalDate;
import java.util.List;

public interface CalculateDailyBalanceUseCase {
    DailyBalance calculateForProvider(CalculateDailyBalanceCommand command);
    List<DailyBalance> calculateForAllProviders(LocalDate date);
}
