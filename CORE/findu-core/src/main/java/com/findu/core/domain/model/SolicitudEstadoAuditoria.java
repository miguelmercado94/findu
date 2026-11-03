package com.findu.core.domain.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudEstadoAuditoria {
    private Long id;
    private Long solicitudServicioId;
    private String estadoAnterior;
    private String estadoNuevo;
    private String actorTipo;
    private Long actorId;
    private BigDecimal latitud;
    private BigDecimal longitud;
    private BigDecimal distanciaMetros;
    private String motivo;
    private LocalDateTime fechaHora;
}

