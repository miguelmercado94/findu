package com.findu.core.domain.model;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerfilEspecialista {
    private Long id;
    private Long perfilProveedorId;
    private Long servicioId;
    private String descripcion;
    private Integer experienciaAnios;
    private BigDecimal calificacionPromedio;
    private boolean active;
}
