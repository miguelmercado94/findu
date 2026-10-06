package com.findu.transaction.dailybalance.config;

import com.findu.transaction.dailybalance.service.DailyBalanceProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class SchedulingConfig {

    private final DailyBalanceProcessor dailyBalanceProcessor;

    @Scheduled(cron = "${findu.daily-balance.cron:0 0 22 * * *}", zone = "${findu.daily-balance.timezone:America/Bogota}")
    public void runScheduledDailyBalance() {
        log.info("Scheduled Task: Ejecutando corte diario automático a las 22:00 (America/Bogota)");
        LocalDate today = LocalDate.now(ZoneId.of("America/Bogota"));
        dailyBalanceProcessor.executeFinancialCut(today, null);
    }
}
