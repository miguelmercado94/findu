package com.findu.core.application.usecase.impl;

import com.findu.core.application.service.EspecialistaCredencialService;
import com.findu.core.application.service.PerfilEspecialistaService;
import com.findu.core.application.usecase.GestionCredencialesUseCase;
import com.findu.core.domain.model.EspecialistaCredencial;
import com.findu.core.dto.request.CrearCredencialRequest;
import com.findu.core.dto.response.EspecialistaCredencialResponse;
import com.findu.core.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class GestionCredencialesUseCaseImpl implements GestionCredencialesUseCase {

    private final EspecialistaCredencialService credencialService;
    private final PerfilEspecialistaService especialistaService;

    @Override
    public EspecialistaCredencialResponse agregarCredencial(CrearCredencialRequest request) {
        // Validar que exista la especialidad
        especialistaService.findById(request.perfilEspecialistaId())
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada con id: " + request.perfilEspecialistaId()));

        String tipoNorm = request.tipoCertificado().toUpperCase().trim();
        
        // Regla de negocio: Si es SUPERIOR, requiere fechaInicio y fechaFin
        if ("SUPERIOR".equals(tipoNorm) && request.fechaInicio() == null) {
            throw new IllegalArgumentException("Para certificados de tipo SUPERIOR es obligatoria la fecha de inicio.");
        }

        EspecialistaCredencial domain = EspecialistaCredencial.builder()
                .perfilEspecialistaId(request.perfilEspecialistaId())
                .tipoCertificado(tipoNorm)
                .nombreTitulo(request.nombreTitulo())
                .institucion(request.institucion())
                .fechaInicio(request.fechaInicio())
                .fechaFin(request.fechaFin())
                .urlCertificadoS3(request.urlCertificadoS3())
                .build();

        EspecialistaCredencial saved = credencialService.save(domain);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EspecialistaCredencialResponse> listarPorEspecialidad(Long perfilEspecialistaId) {
        return credencialService.findByEspecialistaId(perfilEspecialistaId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public void eliminarCredencial(Long id) {
        credencialService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Credencial no encontrada con id: " + id));
        credencialService.deleteById(id);
    }

    private EspecialistaCredencialResponse toResponse(EspecialistaCredencial c) {
        return new EspecialistaCredencialResponse(
                c.getId(),
                c.getPerfilEspecialistaId(),
                c.getTipoCertificado(),
                c.getNombreTitulo(),
                c.getInstitucion(),
                c.getFechaInicio(),
                c.getFechaFin(),
                c.getUrlCertificadoS3()
        );
    }
}
