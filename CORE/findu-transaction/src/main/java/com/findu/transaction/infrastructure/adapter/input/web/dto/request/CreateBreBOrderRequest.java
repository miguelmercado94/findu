package com.findu.transaction.infrastructure.adapter.input.web.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateBreBOrderRequest {

    @NotNull(message = "El monto a pagar es obligatorio")
    @DecimalMin(value = "1000.00", message = "El monto mínimo a pagar con Bre-B es de $1.000 COP")
    private BigDecimal amount;

    private String username;
}
