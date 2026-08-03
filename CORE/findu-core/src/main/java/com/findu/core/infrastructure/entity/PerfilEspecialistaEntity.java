package com.findu.core.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "perfil_especialista")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerfilEspecialistaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "perfil_proveedor_id", nullable = false)
    private Long perfilProveedorId;

    @Column(name = "servicio_id", nullable = false)
    private Long servicioId;

    private String descripcion;

    @Column(name = "experiencia_anios")
    private Integer experienciaAnios;

    @Column(nullable = false, columnDefinition = "boolean default true")\n    @Builder.Default\n    private boolean active = true;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

