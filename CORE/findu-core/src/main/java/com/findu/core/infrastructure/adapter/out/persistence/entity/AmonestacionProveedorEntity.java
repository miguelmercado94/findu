package com.findu.core.infrastructure.adapter.out.persistence.entity;

import com.findu.core.domain.enums.NivelAmonestacion;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "amonestacion_proveedor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AmonestacionProveedorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "perfil_proveedor_id", nullable = false)
    private Long perfilProveedorId;

    @Column(name = "solicitud_servicio_id", nullable = false)
    private Long solicitudServicioId;

    @Column(name = "cancelacion_id", nullable = false, unique = true)
    private Long cancelacionId;

    @Column(name = "ticket_codigo", length = 60)
    private String ticketCodigo;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel", nullable = false, length = 20)
    private NivelAmonestacion nivel;

    @Column(name = "motivo", nullable = false, length = 300)
    private String motivo;

    @Column(name = "conteo_30_dias", nullable = false)
    private Integer conteo30Dias;

    @Column(name = "dias_restriccion", nullable = false)
    @Builder.Default
    private Integer diasRestriccion = 0;

    @Column(name = "restriccion_inicio")
    private LocalDateTime restriccionInicio;

    @Column(name = "restriccion_fin")
    private LocalDateTime restriccionFin;

    @Column(name = "activa")
    @Builder.Default
    private Boolean activa = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}

