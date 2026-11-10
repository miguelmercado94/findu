package com.findu.transaction.infrastructure.adapter.input.scheduler;

import com.findu.transaction.application.port.in.ExecuteAllSettlementsUseCase;
import com.findu.transaction.application.port.in.dto.SettlementBatchResult;
import com.findu.transaction.infrastructure.adapter.out.gateway.MockPayoutGatewayAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SettlementScheduledJobTest {

    private static final ExecuteAllSettlementsUseCase USE_CASE = mock(ExecuteAllSettlementsUseCase.class);

    @Configuration
    @Import({SettlementScheduledJob.class, MockPayoutGatewayAdapter.class})
    static class Config {
        @Bean
        ExecuteAllSettlementsUseCase useCase() { return USE_CASE; }
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(Config.class);

    @Test
    @DisplayName("Con findu.settlement.mock.enabled=true existen el job y la pasarela mock")
    void enabled() {
        runner.withPropertyValues("findu.settlement.mock.enabled=true").run(ctx -> {
            assertThat(ctx).hasSingleBean(SettlementScheduledJob.class);
            assertThat(ctx).hasSingleBean(MockPayoutGatewayAdapter.class);
        });
    }

    @Test
    @DisplayName("Con el flag en false o sin definir no existen ni el job ni la pasarela mock")
    void disabled() {
        runner.withPropertyValues("findu.settlement.mock.enabled=false").run(ctx -> {
            assertThat(ctx).doesNotHaveBean(SettlementScheduledJob.class);
            assertThat(ctx).doesNotHaveBean(MockPayoutGatewayAdapter.class);
        });
        runner.run(ctx -> assertThat(ctx).doesNotHaveBean(SettlementScheduledJob.class));
    }

    @Test
    @DisplayName("El job liquida a todos con el corte por defecto (null)")
    void jobCallsExecuteAll() {
        when(USE_CASE.executeAll(isNull())).thenReturn(
                SettlementBatchResult.builder().cutoffDate(LocalDate.of(2026, 11, 8)).results(List.of()).build());
        new SettlementScheduledJob(USE_CASE).runWeeklySettlement();
        verify(USE_CASE).executeAll(null);
    }
}