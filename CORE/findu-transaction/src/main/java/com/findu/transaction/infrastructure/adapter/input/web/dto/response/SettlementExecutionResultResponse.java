package com.findu.transaction.infrastructure.adapter.input.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class SettlementExecutionResultResponse {
    private String executionId;
    private LocalDate targetDate;
    private int providersProcessed;
    private int settlementsCreated;
    private int settlementsSkipped;
    private int providersFailed;
    private Instant startedAt;
    private Instant finishedAt;
    private String status;
    private List<String> errors;
    // --- Liquidacion simulada (aditivo) ---
    private LocalDate cutoffDate;
    private LocalDate nextSettlementDate;
    private List<ProviderResult> results;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProviderResult {
        private Long providerId;
        /** SETTLED | ALREADY_SETTLED | NOTHING_TO_SETTLE | NET_ZERO_NO_TRANSFER | FAILED */
        private String outcome;
        private String settlementId;
        private String settlementCode;
        private BigDecimal grossAmount;
        private BigDecimal debtCompensationAmount;
        private BigDecimal netAmount;
        private BigDecimal remainingDebt;
        private boolean transferred;
        private String payoutCode;
        private String reference;
        private Instant periodStart;
        private Instant periodEnd;
        private Instant executedAt;
        private int dailyBalancesSettled;
        private String message;
    }
}
