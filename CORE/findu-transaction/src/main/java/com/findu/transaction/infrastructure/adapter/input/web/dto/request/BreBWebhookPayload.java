package com.findu.transaction.infrastructure.adapter.input.web.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BreBWebhookPayload {
    private String orderId;
    private String orderCode;
    private String transactionId;
    private BigDecimal amount;
    private String currency;
    private String status; // CONFIRMED, COMPLETED, FAILED
    private String authorizationCode;
    private String breBKey;
    private Long timestamp;
}
