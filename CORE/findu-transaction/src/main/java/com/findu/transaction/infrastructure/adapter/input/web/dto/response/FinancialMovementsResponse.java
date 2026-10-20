package com.findu.transaction.infrastructure.adapter.input.web.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class FinancialMovementsResponse {
    private String filter; // THIS_WEEK, LAST_30_DAYS, ALL
    private String periodLabel;
    private String currency;
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal netBalance;
    private List<MovementItemDto> movements;

    @Data
    @Builder
    public static class MovementItemDto {
        private String id;
        private String title;
        private String subtitle;
        private Instant date;
        private String dateLabel;
        private BigDecimal amount;
        private String currency;
        private boolean isIncome;
        private String type;
        private String status;
        private String reference;
    }
}
