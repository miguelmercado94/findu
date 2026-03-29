package com.findu.security.infrastructure.adapter.persistence;

import com.findu.security.application.port.output.persistence.UserRolRepositoryPort;
import com.findu.security.domain.model.Rol;
import com.findu.security.infrastructure.entity.UserRolEntity;
import com.findu.security.infrastructure.repository.RoleRepository;
import com.findu.security.infrastructure.repository.UserRolRepository;
import com.findu.security.mapper.RolMapper;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Adapter para la relación usuario-rol. Implementa UserRolRepositoryPort
 * y delega en UserRolRepository. Rellena auditoría antes del save.
 */
@Component
public class UserRolRepoAdapter implements UserRolRepositoryPort {

    private final UserRolRepository userRolRepository;
    private final RoleRepository roleRepository;
    private final RolMapper rolMapper;

    public UserRolRepoAdapter(UserRolRepository userRolRepository, RoleRepository roleRepository, RolMapper rolMapper) {
        this.userRolRepository = userRolRepository;
        this.roleRepository = roleRepository;
        this.rolMapper = rolMapper;
    }

    @Override
    public Mono<Void> assignRoleToUser(Long userId, Integer roleId) {
        UserRolEntity entity = new UserRolEntity();
        entity.setUserId(userId);
        entity.setRoleId(roleId);
        entity.setActive(true);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setCreatedBy("system");
        entity.setUpdatedBy("system");
        return userRolRepository.save(entity).then();
    }

    @Override
    public Mono<Rol> findRoleByUserId(Long userId) {
        return userRolRepository.findByUserId(userId)
                .next()
                .flatMap(ur -> roleRepository.findById(ur.getRoleId())
                        .map(rolMapper::toDomain));
    }
}
