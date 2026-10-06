package com.findu.transaction.infrastructure.adapter.input.web.dto.response;

import com.findu.transaction.domain.enums.RefundStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class RefundResponse {
    private String id;
    private String refundCode;
    private String transactionId;
    private Long customerId;
    private BigDecimal amount;
    private String currency;
    private String reason;
    private RefundStatus status;
    private String externalReference;
    private String failureReason;
    private Instant createdAt;
    private Instant processedAt;
    private Instant completedAt;
}
