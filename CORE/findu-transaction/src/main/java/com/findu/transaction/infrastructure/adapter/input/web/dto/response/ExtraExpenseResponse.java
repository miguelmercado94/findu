package com.findu.transaction.infrastructure.adapter.input.web.dto.response;

import com.findu.transaction.domain.enums.ExtraExpenseStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class ExtraExpenseResponse {
    private String id;
    private Long serviceRequestId;
    private Long providerId;
    private BigDecimal amount;
    private String currency;
    private String reason;
    private ExtraExpenseStatus status;
    private Instant requestedAt;
    private Instant approvedAt;
    private Instant updatedAt;
}
