package com.findu.transaction.infrastructure.adapter.input.web.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreatePayoutRequest {
    @NotNull(message = "El providerId es obligatorio")
    private Long providerId;
    private String settlementId;
    private BigDecimal amount;
}
