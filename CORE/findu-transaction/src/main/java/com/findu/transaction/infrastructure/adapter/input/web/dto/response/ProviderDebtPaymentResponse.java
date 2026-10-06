package com.findu.transaction.infrastructure.adapter.input.web.dto.response;

import com.findu.transaction.domain.enums.PaymentMethod;
import com.findu.transaction.domain.enums.PaymentStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class ProviderDebtPaymentResponse {
    private String id;
    private String paymentCode;
    private Long providerId;
    private BigDecimal amount;
    private String currency;
    private PaymentMethod paymentMethod;
    private String externalReference;
    private PaymentStatus status;
    private Instant createdAt;
}
