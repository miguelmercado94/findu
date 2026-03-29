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
 * Entidad operación. Restricciones de unicidad en BD: name, path.
 */
@Table("OPERATION")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OperationEntity {

    @Id
    private Long id;
    /** Único en la tabla. */
    private String path;
    /** Único en la tabla. */
    private String name;

    @Column("MODULE_ID")
    private Long moduleId;
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
