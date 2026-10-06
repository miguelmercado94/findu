package com.findu.transaction.infrastructure.adapter.input.web.dto.response;

import com.findu.transaction.domain.enums.ProviderType;
import com.findu.transaction.domain.enums.SettlementStatus;
import com.findu.transaction.domain.enums.SettlementType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
public class SettlementResponse {
    private String id;
    private String settlementCode;
    private Long providerId;
    private ProviderType providerType;
    private BigDecimal grossAmount;
    private BigDecimal debtCompensationAmount;
    private BigDecimal netAmount;
    private String currency;
    private SettlementStatus status;
    private SettlementType settlementType;
    private Instant periodStart;
    private Instant periodEnd;
    private Instant createdAt;
    private List<String> dailyBalanceIds;
}
