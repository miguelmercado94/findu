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
 * Entidad usuario. Restricciones de unicidad en BD: email, username, phone.
 */
@Table("\"user\"")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {

    @Id
    private Long id;
    /** Único en la tabla (puede ser null). */
    private String phone;
    @Column("cod_phone_international")
    private String codPhoneInternational;
    /** Único en la tabla. */
    private String username;
    private String password;
    /** Único en la tabla. */
    private String email;
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
