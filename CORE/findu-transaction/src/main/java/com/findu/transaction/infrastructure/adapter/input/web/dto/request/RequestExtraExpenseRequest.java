package com.findu.transaction.infrastructure.adapter.input.web.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RequestExtraExpenseRequest {

    @NotNull(message = "El serviceRequestId es obligatorio")
    private Long serviceRequestId;

    @NotNull(message = "El providerId es obligatorio")
    private Long providerId;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser positivo")
    private BigDecimal amount;

    private String reason;
}
