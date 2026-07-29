package com.findu.core.domain.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Categoria {
    private Long id;
    private String nombre;
    private String descripcion;
    private Long categoriaPadreId;
    private boolean active;
}
