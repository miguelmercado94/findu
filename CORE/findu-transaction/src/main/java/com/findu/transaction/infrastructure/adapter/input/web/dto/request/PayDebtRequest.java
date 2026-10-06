package com.findu.transaction.infrastructure.adapter.input.web.dto.request;

import com.findu.transaction.domain.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PayDebtRequest {

    @NotNull(message = "El providerId es obligatorio")
    private Long providerId;

    @NotNull(message = "El monto a pagar es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser positivo")
    private BigDecimal amount;

    private PaymentMethod paymentMethod;
    private String externalReference;
}
