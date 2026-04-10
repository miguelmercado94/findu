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
@Table("OPERATION")
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

    @Column("HTTP_METHOD")
    /** GET, POST, PUT, DELETE, PATCH, etc. */
    private String httpMethod;

    @Column("MODULE_ID")
    private Long moduleId;

    @Column("PERMITE_ALL")
    private boolean permiteAll;

    private boolean active = true;

    @Column("CREATED_AT")
    @CreatedDate
    private LocalDateTime createdAt;

    @Column("UPDATED_AT")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Column("CREATED_BY")
    @CreatedBy
    private String createdBy;

    @Column("UPDATED_BY")
    @LastModifiedBy
    private String updatedBy;
}
