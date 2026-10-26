package com.findu.core.domain.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Oferta {
    private Long id;
    private Long solicitudServicioId;
    private Long perfilProveedorId;
    private BigDecimal valorPropuesto;
    private LocalTime horaInicioPropuesta;
    private String tiempoEstimado;
    private String mensajePresentacion;
    private String estadoOferta;
}
