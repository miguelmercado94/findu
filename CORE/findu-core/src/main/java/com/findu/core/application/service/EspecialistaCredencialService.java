package com.findu.core.application.service;

import com.findu.core.application.port.output.persistence.EspecialistaCredencialRepositoryPort;
import com.findu.core.domain.model.EspecialistaCredencial;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EspecialistaCredencialService {

    private final EspecialistaCredencialRepositoryPort repositoryPort;

    public EspecialistaCredencial save(EspecialistaCredencial credencial) {
        return repositoryPort.save(credencial);
    }

    public Optional<EspecialistaCredencial> findById(Long id) {
        return repositoryPort.findById(id);
    }

    public List<EspecialistaCredencial> findByEspecialistaId(Long perfilEspecialistaId) {
        return repositoryPort.findByEspecialistaId(perfilEspecialistaId);
    }

    public void deleteById(Long id) {
        repositoryPort.deleteById(id);
    }
}
