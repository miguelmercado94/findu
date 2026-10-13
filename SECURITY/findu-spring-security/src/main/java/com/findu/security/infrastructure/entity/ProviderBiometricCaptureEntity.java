package com.findu.security.infrastructure.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Captura biométrica + la foto frontal que pasa a ser foto de perfil (inmutable).
 */
@Table("provider_biometric_capture")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderBiometricCaptureEntity {

    @Id
    private Long id;

    @Column("user_id")
    private Long userId;

    @Column("cedula_nombre")
    private String cedulaNombre;

    @Column("cedula_fecha_nac")
    private LocalDate cedulaFechaNac;

    @Column("face_match_score")
    private BigDecimal faceMatchScore;

    @Column("foto_frontal_url")
    private String fotoFrontalUrl;

    private String resultado;

    @Column("created_at")
    private LocalDateTime createdAt;
}
