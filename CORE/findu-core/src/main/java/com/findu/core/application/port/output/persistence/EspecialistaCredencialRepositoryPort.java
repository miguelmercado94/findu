package com.findu.core.application.port.output.persistence;

import com.findu.core.domain.model.EspecialistaCredencial;

import java.util.List;
import java.util.Optional;

public interface EspecialistaCredencialRepositoryPort {
    EspecialistaCredencial save(EspecialistaCredencial credencial);
    Optional<EspecialistaCredencial> findById(Long id);
    List<EspecialistaCredencial> findByEspecialistaId(Long perfilEspecialistaId);
    void deleteById(Long id);
}
