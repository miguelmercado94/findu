package com.findu.core.domain.model;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EspecialistaCredencial {
    private Long id;
    private Long perfilEspecialistaId;
    private String tipoCertificado; // SUPERIOR, CERTIFICADO, CURSO, DIPLOMADO
    private String nombreTitulo;
    private String institucion;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String urlCertificadoS3;
}
