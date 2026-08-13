package com.findu.core.application.service.impl;

import com.findu.core.application.port.output.persistence.CategoriaRepositoryPort;
import com.findu.core.application.port.output.persistence.MunicipioRepositoryPort;
import com.findu.core.application.port.output.persistence.ServicioRepositoryPort;
import com.findu.core.application.service.CatalogoService;
import com.findu.core.domain.model.Categoria;
import com.findu.core.domain.model.Municipio;
import com.findu.core.domain.model.Servicio;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    @Cacheable(value = "categorias-page", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<Categoria> findCategorias(Pageable pageable) {
        return categoriaPort.findRootCategorias(pageable);
    }

    @Override
    public Page<Categoria> findSubcategorias(Long categoriaPadreId, Pageable pageable) {
        return categoriaPort.findSubcategorias(categoriaPadreId, pageable);
    }

    @Override
    public List<Servicio> findServiciosByCategoria(Long categoriaId) {
        return servicioPort.findByCategoriaId(categoriaId);
    }

    @Override
    @Cacheable(value = "servicios-page", key = "(#categoriaId ?: 'all') + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<Servicio> findServicios(Long categoriaId, Pageable pageable) {
        if (categoriaId != null) {
            return servicioPort.findByCategoriaId(categoriaId, pageable);
        }
        return servicioPort.findAll(pageable);
    }

    @Override
    @Cacheable(value = "municipios", unless = "#result.isEmpty()")
    public List<Municipio> findAllMunicipios() {
        return municipioPort.findAll();
    }
}
