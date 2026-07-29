package com.findu.core.infrastructure.adapter.persistence;

import com.findu.core.application.port.output.persistence.MunicipioRepositoryPort;
import com.findu.core.domain.model.Municipio;
import com.findu.core.infrastructure.repository.MunicipioRepository;
import com.findu.core.mapper.MunicipioMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MunicipioRepoAdapter implements MunicipioRepositoryPort {

    private final MunicipioRepository repository;
    private final MunicipioMapper mapper;

    @Override
    public List<Municipio> findAll() {
        return repository.findAllByActiveTrue().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Municipio> findByCodigoDane(String codigoDane) {
        return repository.findByCodigoDane(codigoDane).map(mapper::toDomain);
    }
}
