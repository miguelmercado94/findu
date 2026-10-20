package com.findu.transaction.infrastructure.adapter.input.web.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ConfirmBankConsignmentRequest {

    @NotNull(message = "El providerId es obligatorio")
    private Long providerId;

    @NotNull(message = "El monto consignado es obligatorio")
    @DecimalMin(value = "1.00", message = "El monto debe ser mayor a cero")
    private BigDecimal amount;

    @NotBlank(message = "La referencia de pago es obligatoria")
    private String reference;

    private String bankName;
    private String confirmedBy;
    private String notes;
}
