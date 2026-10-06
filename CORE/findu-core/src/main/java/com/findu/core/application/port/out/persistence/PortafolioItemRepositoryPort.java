package com.findu.core.application.port.out.persistence;

import com.findu.core.domain.model.PortafolioItem;

import java.util.List;

public interface PortafolioItemRepositoryPort {

    PortafolioItem save(PortafolioItem portafolioItem);

    List<PortafolioItem> findByEspecialistaId(Long perfilEspecialistaId);

    void deleteById(Long id);
}
