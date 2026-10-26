package com.findu.transaction.application.port.in.finance.dto;

import com.findu.transaction.domain.valueobject.Money;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class WeeklyBalanceResult {
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String periodLabel;
    private final Money provisionalBalance;
    private final String balanceType; // PROVISIONAL, AVAILABLE, NEGATIVE_WARNING, RESTRICTED
    private final int completedServicesCount;
    private final int pendingSettlementCount;
    private final int pagosEnEfectivoCount;
    private final java.time.LocalDateTime fechaInicio;
    private final java.time.LocalDateTime fechaFin;
    private final String cutoffNote;

    // Próxima liquidación
    private final LocalDate nextSettlementDate;
    private final String nextSettlementDateLabel;
    private final String nextSettlementStatus;
    private final Money nextSettlementEstimatedAmount;
    private final String nextSettlementNote;

    // Actividad de la semana
    private final Money completedServicesAmount;
    private final BigDecimal finduCommissionRate;
    private final Money finduCommissionAmount;
    private final Money cashPaymentsReceived;
    private final Money adjustmentsAmount;
    private final Money totalIncome;
    private final Money totalExpenses;
    private final Money netWeekBalance;

    // Deuda / Restricciones
    private final DebtStatusResult debtSummary;
}
