package com.findu.core.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "solicitud_servicio")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudServicioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "perfil_cliente_id", nullable = false)
    private Long perfilClienteId;

    @Column(name = "servicio_id", nullable = false)
    private Long servicioId;

    @Column(name = "direccion_id", nullable = false)
    private Long direccionId;

    @Column(name = "fecha_programada")
    private LocalDateTime fechaProgramada;

    @Column(name = "nombre_contacto")
    private String nombreContacto;

    @Column(name = "telefono_contacto")
    private String telefonoContacto;

    @Builder.Default
    private Integer prioridad = 3;

    @Column(name = "presupuesto_maximo")
    private BigDecimal presupuestoMaximo;

    @Column(name = "cantidad_estimada")
    @Builder.Default
    private Integer cantidadEstimada = 1;

    @Column(name = "estado_solicitud", nullable = false)
    @Builder.Default
    private String estadoSolicitud = "ABIERTA";

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
