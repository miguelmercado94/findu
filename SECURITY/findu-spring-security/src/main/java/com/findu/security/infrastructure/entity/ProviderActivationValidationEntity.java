package com.findu.security.infrastructure.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * Fila de validación de activación (OTP|EMAIL|BIOMETRIA|SOPORTE) con histórico/versionado.
 * Una re-ejecución inserta una nueva versión y la última ({@code is_current=true}) es la vigente.
 */
@Table("provider_activation_validation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderActivationValidationEntity {

    @Id
    private Long id;

    @Column("user_id")
    private Long userId;

    private String tipo;
    private String estado;
    private Integer version;

    @Column("is_current")
    private boolean isCurrent;

    private String detalle;

    @Column("completed_at")
    private LocalDateTime completedAt;

    @Column("created_at")
    @CreatedDate
    private LocalDateTime createdAt;

    @Column("updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Column("created_by")
    @CreatedBy
    private String createdBy;

    @Column("updated_by")
    @LastModifiedBy
    private String updatedBy;
}
