package com.findu.transaction.infrastructure.adapter.input.web.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class WeeklyBalanceResponse {
    private LocalDate startDate;
    private LocalDate endDate;
    private String periodLabel;
    private BigDecimal provisionalBalance;
    private String currency;
    private String balanceType; // PROVISIONAL, AVAILABLE, NEGATIVE_WARNING, RESTRICTED
    private int completedServicesCount;
    private int pendingSettlementCount;
    private String cutoffNote;

    private NextSettlementDto nextSettlement;
    private WeeklyActivityDto weeklyActivity;
    private DebtStatusResponse debtSummary;

    @Data
    @Builder
    public static class NextSettlementDto {
        private LocalDate scheduledDate;
        private String scheduledDateLabel;
        private String status;
        private BigDecimal estimatedAmount;
        private String note;
    }

    @Data
    @Builder
    public static class WeeklyActivityDto {
        private BigDecimal completedServicesAmount;
        private int completedServicesCount;
        private BigDecimal finduCommissionRate;
        private BigDecimal finduCommissionAmount;
        private BigDecimal cashPaymentsReceived;
        private BigDecimal adjustmentsAmount;
        private BigDecimal totalIncome;
        private BigDecimal totalExpenses;
        private BigDecimal netWeekBalance;
    }
}
