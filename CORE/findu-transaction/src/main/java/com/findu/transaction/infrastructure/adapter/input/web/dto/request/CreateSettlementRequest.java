package com.findu.transaction.infrastructure.adapter.input.web.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateSettlementRequest {

    @NotNull(message = "El providerId es obligatorio")
    private Long providerId;

    @NotNull(message = "La fecha inicial de la liquidación es obligatoria")
    private LocalDate startDate;

    @NotNull(message = "La fecha final de la liquidación es obligatoria")
    private LocalDate endDate;
}
