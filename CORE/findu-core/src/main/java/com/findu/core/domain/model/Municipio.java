package com.findu.core.domain.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Municipio {
    private Long id;
    private String codigoDane;
    private String nombre;
    private String departamento;
    private boolean active;
}
