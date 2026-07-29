package com.findu.core.domain.model;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Factura {
    private Long id;
    private Long solicitudServicioId;
    private BigDecimal subtotal;
    private BigDecimal comisionPlataforma;
    private BigDecimal total;
    private String estado;
}
