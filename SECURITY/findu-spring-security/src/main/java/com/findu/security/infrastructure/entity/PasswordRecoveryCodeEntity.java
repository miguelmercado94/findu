package com.findu.security.infrastructure.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("password_recovery_code")
public class PasswordRecoveryCodeEntity {

    @Id
    private Long id;

    @Column("user_id")
    private Long userId;

    private String code;

    @Column("emitted_at")
    private Long emittedAt;

    @Column("expires_at")
    private Long expiresAt;

    private boolean used;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Long getEmittedAt() {
        return emittedAt;
    }

    public void setEmittedAt(Long emittedAt) {
        this.emittedAt = emittedAt;
    }

    public Long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }
}
