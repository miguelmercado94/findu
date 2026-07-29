package com.findu.core.application.usecase.impl;

import com.findu.core.application.service.PerfilEspecialistaService;
import com.findu.core.application.service.PortafolioService;
import com.findu.core.application.usecase.GestionEspecialidadesUseCase;
import com.findu.core.domain.model.PerfilEspecialista;
import com.findu.core.domain.model.PortafolioItem;
import com.findu.core.dto.request.CrearEspecialidadRequest;
import com.findu.core.dto.request.CrearPortafolioRequest;
import com.findu.core.dto.response.PerfilEspecialistaResponse;
import com.findu.core.dto.response.PortafolioItemResponse;
import com.findu.core.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class GestionEspecialidadesUseCaseImpl implements GestionEspecialidadesUseCase {

    private final PerfilEspecialistaService especialistaService;
    private final PortafolioService portafolioService;

    @Override
    public PerfilEspecialistaResponse agregarEspecialidad(CrearEspecialidadRequest request) {
        if (especialistaService.existsByProveedorIdAndServicioId(request.perfilProveedorId(), request.servicioId())) {
            throw new IllegalStateException("El proveedor ya tiene registrada esta especialidad.");
        }

        PerfilEspecialista especialista = PerfilEspecialista.builder()
                .perfilProveedorId(request.perfilProveedorId())
                .servicioId(request.servicioId())
                .descripcion(request.descripcion())
                .experienciaAnios(request.experienciaAnios())
                .active(true)
                .build();

        PerfilEspecialista saved = especialistaService.save(especialista);
        return new PerfilEspecialistaResponse(saved.getId(), null, saved.getDescripcion(), saved.getExperienciaAnios(), saved.isActive());
    }

    @Override
    public void cambiarEstadoEspecialidad(Long id, boolean active) {
        PerfilEspecialista esp = especialistaService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada con id: " + id));
        esp.setActive(active);
        especialistaService.save(esp);
    }

    @Override
    public void eliminarEspecialidad(Long id) {
        especialistaService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada con id: " + id));
        especialistaService.deleteById(id);
    }

    @Override
    public PortafolioItemResponse agregarPortafolio(Long especialidadId, CrearPortafolioRequest request) {
        especialistaService.findById(especialidadId)
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada con id: " + especialidadId));

        PortafolioItem item = PortafolioItem.builder()
                .perfilEspecialistaId(especialidadId)
                .titulo(request.titulo())
                .descripcion(request.descripcion())
                .urlImagen(request.urlImagen())
                .build();

        PortafolioItem saved = portafolioService.save(item);
        return new PortafolioItemResponse(saved.getId(), saved.getTitulo(), saved.getDescripcion(), saved.getUrlImagen());
    }

    @Override
    public void eliminarPortafolio(Long itemId) {
        portafolioService.deleteById(itemId);
    }
}
