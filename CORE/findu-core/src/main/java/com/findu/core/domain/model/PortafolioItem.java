package com.findu.core.domain.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortafolioItem {
    private Long id;
    private Long perfilEspecialistaId;
    private String titulo;
    private String descripcion;
    private String urlImagen;
}
