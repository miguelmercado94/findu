package com.findu.core.application.service;

import com.findu.core.domain.model.PortafolioItem;

import java.util.List;

public interface PortafolioService {

    PortafolioItem save(PortafolioItem portafolioItem);

    List<PortafolioItem> findByEspecialistaId(Long perfilEspecialistaId);

    void deleteById(Long id);
}
