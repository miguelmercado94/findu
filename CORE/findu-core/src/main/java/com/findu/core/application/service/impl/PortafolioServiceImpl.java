package com.findu.core.application.service.impl;

import com.findu.core.application.port.output.persistence.PortafolioItemRepositoryPort;
import com.findu.core.application.service.PortafolioService;
import com.findu.core.domain.model.PortafolioItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PortafolioServiceImpl implements PortafolioService {

    private final PortafolioItemRepositoryPort port;

    @Override
    public PortafolioItem save(PortafolioItem portafolioItem) {
        return port.save(portafolioItem);
    }

    @Override
    public List<PortafolioItem> findByEspecialistaId(Long perfilEspecialistaId) {
        return port.findByEspecialistaId(perfilEspecialistaId);
    }

    @Override
    public void deleteById(Long id) {
        port.deleteById(id);
    }
}
