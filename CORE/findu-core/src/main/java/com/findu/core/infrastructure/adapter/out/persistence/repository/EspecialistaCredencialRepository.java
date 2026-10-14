package com.findu.core.infrastructure.adapter.out.persistence.repository;

import com.findu.core.infrastructure.adapter.out.persistence.entity.EspecialistaCredencialEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EspecialistaCredencialRepository extends JpaRepository<EspecialistaCredencialEntity, Long> {
    List<EspecialistaCredencialEntity> findAllByPerfilEspecialistaId(Long perfilEspecialistaId);

    void deleteAllByPerfilEspecialistaId(Long perfilEspecialistaId);
}
