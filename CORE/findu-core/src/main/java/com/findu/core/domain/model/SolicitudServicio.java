package com.findu.core.domain.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudServicio {
    private Long id;
    private Long perfilClienteId;
    private Long servicioId;
    private Long direccionId;
    private LocalDateTime fechaProgramada;
    private String nombreContacto;
    private String telefonoContacto;
    private Integer prioridad;
    private BigDecimal presupuestoMaximo;
    private Integer cantidadEstimada;
    private String estadoSolicitud;
}
