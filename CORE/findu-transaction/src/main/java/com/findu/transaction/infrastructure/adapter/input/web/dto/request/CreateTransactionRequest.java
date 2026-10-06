package com.findu.transaction.infrastructure.adapter.input.web.dto.request;

import com.findu.transaction.domain.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateTransactionRequest {

    @NotNull(message = "El serviceRequestId es obligatorio")
    private Long serviceRequestId;

    @NotNull(message = "El providerId es obligatorio")
    private Long providerId;

    @NotNull(message = "El customerId es obligatorio")
    private Long customerId;

    @NotNull(message = "El monto del servicio es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto del servicio debe ser mayor a 0")
    private BigDecimal serviceAmount;

    private BigDecimal commissionRate;
    private BigDecimal tipAmount;
    private BigDecimal extraAmount;

    @NotNull(message = "El método de pago es obligatorio")
    private PaymentMethod paymentMethod;
}
