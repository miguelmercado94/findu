package com.findu.core.infrastructure.adapter.out.persistence.entity;

import com.findu.core.domain.enums.UnidadDuracionOferta;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "duracion_unidad", nullable = false, length = 20)
    @Builder.Default
    private UnidadDuracionOferta duracionUnidad = UnidadDuracionOferta.HORAS;

    @Column(name = "duracion_cantidad", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal duracionCantidad = BigDecimal.ONE;

    @Column(name = "mismo_horario_dias")
    @Builder.Default
    private Boolean mismoHorarioDias = false;

    @OneToMany(mappedBy = "oferta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("fecha ASC, horaInicio ASC")
    @Builder.Default
    private List<OfertaCronogramaDiaEntity> cronogramaDias = new ArrayList<>();

    @Column(name = "estado_oferta", nullable = false)
    @Builder.Default
    private String estadoOferta = "ENVIADA";

    @Column(name = "ofertado_at")
    private LocalDateTime ofertadoAt;

    @Column(name = "solicitud_modificada")
    @Builder.Default
    private Boolean solicitudModificada = false;

    @Column(name = "campos_modificados", length = 500)
    private String camposModificados;

    @Column(name = "valores_anteriores", columnDefinition = "TEXT")
    private String valoresAnteriores;

    @Column(name = "solapamiento_horario")
    @Builder.Default
    private Boolean solapamientoHorario = false;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersistDefaults() {
        if (ofertadoAt == null) {
            ofertadoAt = LocalDateTime.now();
        }
        if (solicitudModificada == null) {
            solicitudModificada = false;
        }
        if (solapamientoHorario == null) {
            solapamientoHorario = false;
        }
    }
}
