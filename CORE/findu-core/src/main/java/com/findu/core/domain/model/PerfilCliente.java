package com.findu.core.domain.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerfilCliente {
    private Long id;
    private Long authUserId;
    private String nombreCompleto;
    private String numeroIdentificacion;
    private String tipoIdentificacion;
    private LocalDate fechaNacimiento;
    private String sexo;
    private String celular;
    private String codPhoneInternational;
    private String urlImagenPerfil;
    private BigDecimal calificacionPromedio;
    private String estado;
}
