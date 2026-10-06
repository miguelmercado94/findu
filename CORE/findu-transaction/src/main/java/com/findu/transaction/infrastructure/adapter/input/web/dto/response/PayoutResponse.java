package com.findu.transaction.infrastructure.adapter.input.web.dto.response;

import com.findu.transaction.domain.enums.PayoutStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class PayoutResponse {
    private String id;
    private String payoutCode;
    private String settlementId;
    private Long providerId;
    private BigDecimal amount;
    private String currency;
    private PayoutStatus status;
    private String externalReference;
    private String failureReason;
    private Instant createdAt;
    private Instant processedAt;
    private Instant completedAt;
}
