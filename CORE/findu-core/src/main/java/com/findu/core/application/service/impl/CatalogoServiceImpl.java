package com.findu.core.application.service.impl;

import com.findu.core.application.port.output.persistence.CategoriaRepositoryPort;
import com.findu.core.application.port.output.persistence.MunicipioRepositoryPort;
import com.findu.core.application.port.output.persistence.ServicioRepositoryPort;
import com.findu.core.application.service.CatalogoService;
import com.findu.core.domain.model.Categoria;
import com.findu.core.domain.model.Municipio;
import com.findu.core.domain.model.Servicio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogoServiceImpl implements CatalogoService {

    private final CategoriaRepositoryPort categoriaPort;
    private final ServicioRepositoryPort servicioPort;
    private final MunicipioRepositoryPort municipioPort;

    @Override
    public List<Categoria> findAllCategorias() {
        return categoriaPort.findAll();
    }

    @Override
    public List<Servicio> findServiciosByCategoria(Long categoriaId) {
        return servicioPort.findByCategoriaId(categoriaId);
    }

    @Override
    public List<Municipio> findAllMunicipios() {
        return municipioPort.findAll();
    }
}
