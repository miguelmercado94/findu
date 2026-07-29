package com.findu.core.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "factura_detalle")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacturaDetalleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "factura_id", nullable = false)
    private Long facturaId;

    @Column(nullable = false)
    private String concepto;

    @Column(nullable = false)
    private BigDecimal valor;

    @Column(nullable = false)
    @Builder.Default
    private String tipo = "BASE";

    @Column(name = "estado_aprobacion", nullable = false)
    @Builder.Default
    private String estadoAprobacion = "APROBADO";
}
