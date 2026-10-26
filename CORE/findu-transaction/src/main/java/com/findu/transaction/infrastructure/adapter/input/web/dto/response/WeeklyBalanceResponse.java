package com.findu.transaction.infrastructure.adapter.input.web.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyBalanceResponse {

    // --- Contrato JSON esperado por la App Móvil (Campos aplanados en español) ---
    private String periodo;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaInicio;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaFin;

    private BigDecimal montoBalance;
    private int serviciosCompletados;
    private int serviciosPendientesLiquidar;
    private int pagosEnEfectivoCount;
    private BigDecimal totalServiciosMonto;
    private BigDecimal comisionesFinduMonto;
    private BigDecimal comisionPorcentaje;
    private BigDecimal pagosEfectivoMonto;
    private BigDecimal ajustesMonto;
    private String proximaLiquidacionFecha;
    private BigDecimal proximaLiquidacionMonto;
    private String proximaLiquidacionEstado;
    private BigDecimal limiteDeuda;
    private BigDecimal deudaPendiente;
    private String estadoFinanciero;
    private String restrictionStatus;

    @JsonProperty("isBlocked")
    private boolean isBlocked;

    private String restrictionMessage;
    private String cutoffNote;

    // --- Campos complementarios / retrocompatibilidad ---
    private LocalDate startDate;
    private LocalDate endDate;
    private String periodLabel;
    private BigDecimal provisionalBalance;
    private String currency;
    private String balanceType; // PROVISIONAL, AVAILABLE, NEGATIVE_WARNING, RESTRICTED
    private int completedServicesCount;
    private int pendingSettlementCount;

    private NextSettlementDto nextSettlement;
    private WeeklyActivityDto weeklyActivity;
    private DebtStatusResponse debtSummary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NextSettlementDto {
        private LocalDate scheduledDate;
        private String scheduledDateLabel;
        private String status;
        private BigDecimal estimatedAmount;
        private String note;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
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
