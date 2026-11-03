package com.findu.core.domain.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfertaCronogramaDia {
    private Long id;
    private Long ofertaId;
    private LocalDate fecha;
    private String diaSemana;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private BigDecimal horasTrabajo;
}

