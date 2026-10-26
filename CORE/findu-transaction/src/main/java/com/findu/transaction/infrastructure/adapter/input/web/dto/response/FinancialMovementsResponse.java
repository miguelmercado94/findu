package com.findu.transaction.infrastructure.adapter.input.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialMovementsResponse {
    private String filter; // THIS_WEEK, LAST_30_DAYS, ALL
    private String periodLabel;
    private String currency;
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal netBalance;
    private Integer completedServicesCount;
    private Integer activeDaysCount;
    private List<MovementItemDto> movements;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MovementItemDto {
        private String id;
        private Long serviceRequestId;
        private String serviceCode;
        private String serviceName;
        private String specializationName;
        private String title;
        private String subtitle;
        private Instant date;
        private String dateLabel;
        private String timeLabel;
        private BigDecimal amount;
        private String currency;
        private boolean isIncome;
        private String type;
        private String status;
        private String reference;
    }
}
