package com.findu.transaction.infrastructure.adapter.input.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/** Saldo pendiente neto con desglose; mismo objeto en weekly-balance y movements. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingBalanceResponse {
    private BigDecimal grossAmount;
    private BigDecimal debtCompensationAmount;
    private BigDecimal netAmount;
    private BigDecimal remainingDebt;
    private BigDecimal thisWeekGrossAmount;
    private BigDecimal carriedOverGrossAmount;
    private boolean carriedOver;
    private int pendingServicesCount;
    private String currency;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LastSettlementDto {
        private String settlementCode;
        private String status;
        private BigDecimal grossAmount;
        private BigDecimal debtCompensationAmount;
        private BigDecimal netAmount;
        private Instant executedAt;
        private String reference;
    }
}
