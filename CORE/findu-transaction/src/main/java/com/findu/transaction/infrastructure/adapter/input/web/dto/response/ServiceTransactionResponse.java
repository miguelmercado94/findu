package com.findu.transaction.infrastructure.adapter.input.web.dto.response;

import com.findu.transaction.domain.enums.PaymentMethod;
import com.findu.transaction.domain.enums.PaymentStatus;
import com.findu.transaction.domain.enums.TransactionStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class ServiceTransactionResponse {
    private String id;
    private String transactionCode;
    private Long serviceRequestId;
    private Long providerId;
    private Long customerId;
    private BigDecimal serviceAmount;
    private BigDecimal commissionRate;
    private BigDecimal commissionAmount;
    private BigDecimal tipAmount;
    private BigDecimal extraAmount;
    private BigDecimal totalCustomerAmount;
    private BigDecimal providerAmount;
    private String currency;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private TransactionStatus transactionStatus;
    private Instant serviceCompletedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
