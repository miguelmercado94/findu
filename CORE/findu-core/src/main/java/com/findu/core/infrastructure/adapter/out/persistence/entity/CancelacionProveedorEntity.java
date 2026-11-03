package com.findu.core.infrastructure.adapter.out.persistence.entity;

import com.findu.core.domain.enums.CategoriaCancelacionProveedor;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "cancelacion_proveedor")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancelacionProveedorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "solicitud_servicio_id", nullable = false)
    private Long solicitudServicioId;

    @Column(name = "oferta_id")
    private Long ofertaId;

    @Column(name = "perfil_proveedor_id", nullable = false)
    private Long perfilProveedorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", nullable = false, length = 40)
    private CategoriaCancelacionProveedor categoria;

    @Column(name = "motivo", nullable = false, length = 200)
    private String motivo;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "evidencias_urls", columnDefinition = "TEXT")
    private String evidenciasUrls;

    @Column(name = "ticket_codigo", length = 60)
    private String ticketCodigo;

    @Column(name = "estado_cancelacion", nullable = false, length = 40)
    private String estadoCancelacion;

    @Column(name = "estado_ticket", nullable = false, length = 40)
    private String estadoTicket;

    @Column(name = "es_injustificada")
    private Boolean esInjustificada;

    @Column(name = "fecha_limite_evidencia")
    private LocalDateTime fechaLimiteEvidencia;

    @Column(name = "decision_soporte", length = 40)
    private String decisionSoporte;

    @Column(name = "justificacion_soporte", columnDefinition = "TEXT")
    private String justificacionSoporte;

    @Column(name = "agente_soporte_id")
    private Long agenteSoporteId;

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    @Column(name = "amonestacion_generada")
    @Builder.Default
    private Boolean amonestacionGenerada = false;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @LastModifiedDate
    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}

