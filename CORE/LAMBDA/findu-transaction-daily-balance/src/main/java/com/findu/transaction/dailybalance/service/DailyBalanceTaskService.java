package com.findu.transaction.dailybalance.service;

import com.findu.transaction.dailybalance.model.DailyBalanceExecutionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyBalanceTaskService {

    private final DailyBalanceProcessor dailyBalanceProcessor;

    public DailyBalanceExecutionResult runDailyBalance(LocalDate balanceDate, Long providerId) {
        log.info("DailyBalanceTaskService: Ejecutando corte diario para fecha={} y providerId={}", balanceDate, providerId);
        return dailyBalanceProcessor.executeFinancialCut(balanceDate, providerId);
    }
}
