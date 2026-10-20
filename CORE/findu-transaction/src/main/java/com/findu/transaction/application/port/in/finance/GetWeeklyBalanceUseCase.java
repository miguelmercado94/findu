package com.findu.transaction.application.port.in.finance;

import com.findu.transaction.application.port.in.finance.dto.WeeklyBalanceResult;

import java.time.LocalDate;

public interface GetWeeklyBalanceUseCase {
    WeeklyBalanceResult getWeeklyBalance(Long providerId, LocalDate targetDate, String username);
}
