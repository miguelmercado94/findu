package com.findu.transaction.dailybalance.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class DailyBalanceExecutionResult {
    private final String executionId;
    private final LocalDate balanceDate;
    private final int providersProcessed;
    private final int providersSucceeded;
    private final int providersFailed;
    private final Instant startedAt;
    private final Instant finishedAt;
    private final String status;
    private final List<String> errors;
}
