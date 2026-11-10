package com.findu.transaction.infrastructure.adapter.input.scheduler;

import com.findu.transaction.application.port.in.ExecuteAllSettlementsUseCase;
import com.findu.transaction.application.port.in.dto.SettlementBatchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job de liquidacion semanal simulada (martes 06:00 America/Bogota por defecto).
 * Solo existe con {@code findu.settlement.mock.enabled=true} (local/dev); desactivado por defecto en qa/prod.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "findu.settlement.mock.enabled", havingValue = "true")
public class SettlementScheduledJob {

    private final ExecuteAllSettlementsUseCase executeAllSettlementsUseCase;

    @Scheduled(cron = "${findu.settlement.cron:0 0 6 * * TUE}", zone = "${findu.settlement.zone:America/Bogota}")
    public void runWeeklySettlement() {
        SettlementBatchResult result = executeAllSettlementsUseCase.executeAll(null);
        log.info("Job de liquidacion semanal: corte={} proveedores={}", result.getCutoffDate(), result.getResults().size());
    }
}
