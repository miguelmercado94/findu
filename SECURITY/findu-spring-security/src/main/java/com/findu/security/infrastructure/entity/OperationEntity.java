package com.findu.security.infrastructure.entity;

import lombok.AllArgsConstructor;
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
 * Entidad operación: ruta relativa al micro + método HTTP; {@code name} es la authority en Spring.
 */
@Table("operation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OperationEntity {

    @Id
    private Long id;
    /** Resto de la URL (ej. /api/v1/customers) sin el segmento del micro. */
    private String path;
    /** Authority (ej. CUST_REGISTER). */
    private String name;

    @Column("http_method")
    /** GET, POST, PUT, DELETE, PATCH, etc. */
    private String httpMethod;

    @Column("module_id")
    private Long moduleId;

    @Column("permite_all")
    private boolean permiteAll;

    private boolean active = true;

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
