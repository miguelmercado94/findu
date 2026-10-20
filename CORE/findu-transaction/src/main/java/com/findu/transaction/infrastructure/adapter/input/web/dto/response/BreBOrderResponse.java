package com.findu.transaction.infrastructure.adapter.input.web.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class BreBOrderResponse {
    private String orderId;
    private String orderCode;
    private Long providerId;
    private BigDecimal amount;
    private String currency;
    private String breBKey;
    private String qrPayload;
    private String status; // PENDING, CONFIRMED, EXPIRED, CANCELLED
    private Instant expiresAt;
    private long secondsRemaining;
    private Instant confirmedAt;
    private String externalTransactionId;
    private Instant createdAt;
}
