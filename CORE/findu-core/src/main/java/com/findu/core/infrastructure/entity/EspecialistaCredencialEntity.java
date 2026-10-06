package com.findu.core.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "especialista_credenciales")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EspecialistaCredencialEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "perfil_especialista_id", nullable = false)
    private Long perfilEspecialistaId;

    @Column(name = "tipo_certificado", nullable = false)
    private String tipoCertificado; // SUPERIOR, CERTIFICADO, CURSO, DIPLOMADO

    @Column(name = "nombre_titulo", nullable = false)
    private String nombreTitulo;

    @Column(name = "institucion", nullable = false)
    private String institucion;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(name = "url_certificado_s3", columnDefinition = "TEXT")
    private String urlCertificadoS3;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
