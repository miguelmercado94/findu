package com.findu.transaction.infrastructure.config;

import com.findu.transaction.domain.service.SettlementSchedule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@Configuration
@EnableScheduling
public class SettlementSchedulingConfig {

    /** Reloj en zona America/Bogota: define "hoy" para corte y proxima liquidacion. */
    @Bean
    @ConditionalOnMissingBean(Clock.class)
    public Clock settlementClock() {
        return Clock.system(SettlementSchedule.BOGOTA);
    }
}
