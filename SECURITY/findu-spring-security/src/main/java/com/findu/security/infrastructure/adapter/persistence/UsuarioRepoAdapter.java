package com.findu.security.infrastructure.adapter.persistence;

import com.findu.security.application.port.output.persistence.UsuarioRepositoryPort;
import com.findu.security.domain.model.Usuario;
import com.findu.security.infrastructure.entity.UserEntity;
import com.findu.security.infrastructure.repository.UserRepository;
import com.findu.security.mapper.UsuarioMapper;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Adapter de persistencia para Usuario. Conecta el modelo de dominio con la BD:
 * implementa UsuarioRepositoryPort, usa UsuarioMapper (domain ↔ entity) y delega en UserRepository.
 * <p>
 * Convención: la responsabilidad de rellenar los campos de auditoría antes del save es del RepoAdapter.
 * Siempre se deben llenar: en creación (createdAt, updatedAt, createdBy, updatedBy) y en actualización (updatedAt, updatedBy).
 */
@Component
public class UsuarioRepoAdapter implements UsuarioRepositoryPort {

    private static final String AUDIT_USER = "system";

    private final UserRepository userRepository;
    private final UsuarioMapper usuarioMapper;

    public UsuarioRepoAdapter(UserRepository userRepository, UsuarioMapper usuarioMapper) {
        this.userRepository = userRepository;
        this.usuarioMapper = usuarioMapper;
    }

    @Override
    public Flux<Usuario> findAll() {
        return userRepository.findAll()
                .map(usuarioMapper::toDomain);
    }

    @Override
    public Mono<Usuario> findById(Long id) {
        return userRepository.findById(id)
                .map(usuarioMapper::toDomain);
    }

    @Override
    public Mono<Usuario> getByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(usuarioMapper::toDomain);
    }

    @Override
    public Mono<Usuario> findByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(usuarioMapper::toDomain);
    }

    @Override
    public Mono<Usuario> save(Usuario usuario) {
        if (usuario.getId() == null) {
            UserEntity entity = usuarioMapper.toEntity(usuario);
            LocalDateTime now = LocalDateTime.now();
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            entity.setCreatedBy(entity.getCreatedBy() != null ? entity.getCreatedBy() : AUDIT_USER);
            entity.setUpdatedBy(entity.getUpdatedBy() != null ? entity.getUpdatedBy() : AUDIT_USER);
            return userRepository.save(entity)
                    .map(usuarioMapper::toDomain);
        } else {
            return userRepository.findById(usuario.getId())
                    .flatMap(existingEntity -> {
                        existingEntity.setUsername(usuario.getUsername());
                        existingEntity.setEmail(usuario.getEmail());
                        existingEntity.setPhone(usuario.getPhone());
                        existingEntity.setCodPhoneInternational(usuario.getCodPhoneInternational());
                        existingEntity.setPassword(usuario.getPassword());
                        existingEntity.setActive(usuario.isActive());
                        existingEntity.setUpdatedAt(LocalDateTime.now());
                        existingEntity.setUpdatedBy(AUDIT_USER);
                        return userRepository.save(existingEntity);
                    })
                    .map(usuarioMapper::toDomain);
        }
    }

    @Override
    public Mono<Boolean> existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public Mono<Boolean> existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public Mono<Boolean> existsByPhone(String phone) {
        return userRepository.existsByPhone(phone);
    }

    @Override
    public Mono<Usuario> findByCodPhoneInternationalAndPhone(String codPhoneInternational, String phone) {
        return userRepository.findByCodPhoneInternationalAndPhone(codPhoneInternational, phone)
                .map(usuarioMapper::toDomain);
    }

    @Override
    public Mono<Void> updatePassword(Long userId, String encodedPassword) {
        return userRepository.findById(userId)
                .flatMap(entity -> {
                    entity.setPassword(encodedPassword);
                    entity.setUpdatedAt(LocalDateTime.now());
                    entity.setUpdatedBy(entity.getUpdatedBy() != null ? entity.getUpdatedBy() : AUDIT_USER);
                    return userRepository.save(entity);
                })
                .then();
    }
}
