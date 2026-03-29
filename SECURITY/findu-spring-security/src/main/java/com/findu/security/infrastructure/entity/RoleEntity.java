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
 * Entidad rol. Restricción de unicidad en BD: name.
 */
@Table("ROLE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoleEntity {

    @Id
    private Integer id;
    /** Único en la tabla. */
    private String name;
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
