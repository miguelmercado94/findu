package com.findu.core.domain.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Servicio {
    private Long id;
    private String nombre;
    private String descripcion;
    private String tipoCobro;
    private Long categoriaId;
    private boolean active;
}
