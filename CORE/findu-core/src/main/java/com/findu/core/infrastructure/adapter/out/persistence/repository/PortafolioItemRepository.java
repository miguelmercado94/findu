package com.findu.core.infrastructure.adapter.out.persistence.repository;

import com.findu.core.infrastructure.adapter.out.persistence.entity.PortafolioItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PortafolioItemRepository extends JpaRepository<PortafolioItemEntity, Long> {

    List<PortafolioItemEntity> findAllByPerfilEspecialistaId(Long perfilEspecialistaId);
}
