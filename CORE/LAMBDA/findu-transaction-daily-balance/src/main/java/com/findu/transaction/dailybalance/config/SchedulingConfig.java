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

    // La liquidacion semanal (martes, simulada) la ejecuta findu-transaction (findu.settlement.cron);
    // este lambda solo hace el corte diario.
}
