package com.findu.core.domain.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Calificacion {
    private Long id;
    private Long solicitudServicioId;
    private Long evaluadorId;
    private Long evaluadoId;
    private String tipoEvaluacion;
    private Integer puntaje;
    private String comentario;
}
