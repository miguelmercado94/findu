package com.findu.transaction.infrastructure.adapter.input.web.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateRefundRequest {

    @NotBlank(message = "El transactionId es obligatorio")
    private String transactionId;

    @NotNull(message = "El customerId es obligatorio")
    private Long customerId;

    @NotNull(message = "El monto a devolver es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser positivo")
    private BigDecimal amount;

    private String reason;
}
