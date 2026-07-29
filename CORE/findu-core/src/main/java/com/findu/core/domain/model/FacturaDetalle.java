package com.findu.core.domain.model;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacturaDetalle {
    private Long id;
    private Long facturaId;
    private String concepto;
    private BigDecimal valor;
    private String tipo;
    private String estadoAprobacion;
}
