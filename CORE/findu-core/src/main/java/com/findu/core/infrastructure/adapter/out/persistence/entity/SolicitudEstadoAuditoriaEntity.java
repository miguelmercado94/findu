package com.findu.core.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "solicitud_estado_auditoria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudEstadoAuditoriaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "solicitud_servicio_id", nullable = false)
    private Long solicitudServicioId;

    @Column(name = "estado_anterior", length = 40)
    private String estadoAnterior;

    @Column(name = "estado_nuevo", nullable = false, length = 40)
    private String estadoNuevo;

    @Column(name = "actor_tipo", length = 30)
    private String actorTipo;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "latitud", precision = 10, scale = 7)
    private BigDecimal latitud;

    @Column(name = "longitud", precision = 10, scale = 7)
    private BigDecimal longitud;

    @Column(name = "distancia_metros", precision = 10, scale = 2)
    private BigDecimal distanciaMetros;

    @Column(name = "motivo", length = 500)
    private String motivo;

    @Column(name = "fecha_hora", nullable = false)
    @Builder.Default
    private LocalDateTime fechaHora = LocalDateTime.now();
}

