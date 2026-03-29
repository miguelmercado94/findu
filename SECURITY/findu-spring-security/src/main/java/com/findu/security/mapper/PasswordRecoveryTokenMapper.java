package com.findu.security.mapper;

import com.findu.security.domain.model.PasswordRecoveryToken;
import com.findu.security.infrastructure.entity.PasswordRecoveryTokenEntity;
import org.springframework.stereotype.Component;

/**
 * Mapeo entre PasswordRecoveryToken (dominio) y PasswordRecoveryTokenEntity.
 */
@Component
public class PasswordRecoveryTokenMapper {

    public PasswordRecoveryToken toDomain(PasswordRecoveryTokenEntity entity) {
        if (entity == null) {
            return null;
        }
        PasswordRecoveryToken domain = new PasswordRecoveryToken();
        domain.setId(entity.getId());
        domain.setUserId(entity.getUserId());
        domain.setToken(entity.getToken());
        domain.setExpiresAt(entity.getExpiresAt());
        domain.setUsed(entity.isUsed());
        return domain;
    }

    public PasswordRecoveryTokenEntity toEntity(PasswordRecoveryToken domain) {
        if (domain == null) {
            return null;
        }
        PasswordRecoveryTokenEntity entity = new PasswordRecoveryTokenEntity();
        entity.setId(domain.getId());
        entity.setUserId(domain.getUserId());
        entity.setToken(domain.getToken());
        entity.setExpiresAt(domain.getExpiresAt());
        entity.setUsed(domain.isUsed());
        return entity;
    }
}
