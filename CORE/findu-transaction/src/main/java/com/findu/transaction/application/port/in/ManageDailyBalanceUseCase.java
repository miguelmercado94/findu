package com.findu.transaction.application.port.in;

import com.findu.transaction.domain.model.balance.DailyBalance;

import java.time.LocalDate;
import java.util.List;

public interface ManageDailyBalanceUseCase {
    DailyBalance calculateDailyBalance(Long providerId, LocalDate balanceDate);
    List<DailyBalance> calculateAllDailyBalances(LocalDate balanceDate);
}
