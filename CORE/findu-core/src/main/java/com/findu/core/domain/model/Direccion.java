package com.findu.core.domain.model;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Direccion {
    private Long id;
    private Long perfilClienteId;
    private Long perfilProveedorId;
    private Long solicitudServicioId;
    private String etiqueta;
    private String direccionTexto;
    private Long municipioId;
    private BigDecimal latitud;
    private BigDecimal longitud;
    private String piso;
    private String apartamento;
    private String referencia;
    private boolean esPrincipal;
    private boolean active;
}
