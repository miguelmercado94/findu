package com.findu.core.infrastructure.adapter.out.persistence.repository;

import com.findu.core.infrastructure.adapter.out.persistence.entity.ProveedorCoberturaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProveedorCoberturaRepository extends JpaRepository<ProveedorCoberturaEntity, Long> {

    List<ProveedorCoberturaEntity> findAllByPerfilProveedorId(Long perfilProveedorId);

    List<ProveedorCoberturaEntity> findAllByMunicipioId(Long municipioId);

    void deleteAllByPerfilProveedorId(Long perfilProveedorId);
}
