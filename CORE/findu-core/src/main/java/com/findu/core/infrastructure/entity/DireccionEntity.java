package com.findu.core.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "direccion")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DireccionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "perfil_cliente_id")
    private Long perfilClienteId;

    @Column(name = "perfil_proveedor_id")
    private Long perfilProveedorId;

    @Column(name = "solicitud_servicio_id")
    private Long solicitudServicioId;

    private String etiqueta;

    @Column(name = "direccion_texto", nullable = false)
    private String direccionTexto;

    @Column(name = "municipio_id", nullable = false)
    private Long municipioId;

    private BigDecimal latitud;

    private BigDecimal longitud;

    private String piso;

    private String apartamento;

    private String referencia;

    @Column(name = "es_principal")
    private boolean esPrincipal;

    @Column(nullable = false)
    private boolean active;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
