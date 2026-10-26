package com.findu.core.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "oferta")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfertaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "solicitud_servicio_id", nullable = false)
    private Long solicitudServicioId;

    @Column(name = "perfil_proveedor_id", nullable = false)
    private Long perfilProveedorId;

    @Column(name = "valor_propuesto", nullable = false)
    private BigDecimal valorPropuesto;

    @Column(name = "hora_inicio_propuesta")
    private LocalTime horaInicioPropuesta;

    @Column(name = "tiempo_estimado")
    private String tiempoEstimado;

    @Column(name = "mensaje_presentacion")
    private String mensajePresentacion;

    @Column(name = "estado_oferta", nullable = false)
    @Builder.Default
    private String estadoOferta = "ENVIADA";

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
