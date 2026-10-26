package com.findu.transaction.dailybalance.config;

import com.findu.transaction.dailybalance.service.DailyBalanceProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j
@Configuration
@EnableScheduling
public class SchedulingConfig {

    private final DailyBalanceProcessor dailyBalanceProcessor;
    private final RestClient transactionRestClient;

    public SchedulingConfig(
            DailyBalanceProcessor dailyBalanceProcessor,
            @Value("${findu.transaction.url:http://findu-transaction:8086}") String transactionServiceUrl) {
        this.dailyBalanceProcessor = dailyBalanceProcessor;
        this.transactionRestClient = RestClient.builder()
                .baseUrl(transactionServiceUrl)
                .build();
    }

    /**
     * Corte diario a la medianoche / hora configurada:
     * Calcula balances diarios tanto internamente como invocando findu-transaction.
     */
    @Scheduled(cron = "${findu.daily-balance.cron:0 0 0 * * *}", zone = "${findu.daily-balance.timezone:America/Bogota}")
    public void runScheduledDailyBalance() {
        log.info("Scheduled Task: Ejecutando corte diario automático (America/Bogota)");
        LocalDate today = LocalDate.now(ZoneId.of("America/Bogota"));
        try {
            dailyBalanceProcessor.executeFinancialCut(today, null);
        } catch (Exception e) {
            log.warn("Aviso procesando balance diario localmente: {}", e.getMessage());
        }

        try {
            transactionRestClient.post()
                    .uri("/api/v1/daily-balances/calculate-all?balanceDate={date}", today)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Invocación exitosa a POST /api/v1/daily-balances/calculate-all en findu-transaction");
        } catch (Exception e) {
            log.warn("No se pudo invocar findu-transaction para cálculo diario: {}", e.getMessage());
        }
    }

    /**
     * Corte semanal de Settlements:
     * Se ejecuta los domingos a las 23:59 para consolidar el corte semanal y preparar la dispersión del martes.
     */
    @Scheduled(cron = "${findu.settlement.cron:0 59 23 * * SUN}", zone = "${findu.daily-balance.timezone:America/Bogota}")
    public void runScheduledWeeklySettlement() {
        log.info("Scheduled Task: Ejecutando corte semanal de liquidaciones los domingos (America/Bogota)");
        LocalDate today = LocalDate.now(ZoneId.of("America/Bogota"));
        try {
            transactionRestClient.post()
                    .uri("/internal/settlements/run?date={date}&type=WEEKLY", today)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Invocación exitosa a POST /internal/settlements/run en findu-transaction");
        } catch (Exception e) {
            log.warn("No se pudo invocar POST /internal/settlements/run en findu-transaction: {}", e.getMessage());
        }
    }
}
