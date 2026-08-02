package com.findu.core.application.usecase.impl;

import com.findu.core.application.service.DireccionService;
import com.findu.core.application.service.PerfilClienteService;
import com.findu.core.application.usecase.GestionPerfilClienteUseCase;
import com.findu.core.domain.model.Direccion;
import com.findu.core.domain.model.PerfilCliente;
import com.findu.core.dto.request.ActualizarPerfilClienteRequest;
import com.findu.core.dto.request.CrearPerfilClienteRequest;
import com.findu.core.dto.response.DireccionResponse;
import com.findu.core.dto.response.PerfilClienteDetalleResponse;
import com.findu.core.dto.response.PerfilClienteResponse;
import com.findu.core.exception.ResourceNotFoundException;
import com.findu.core.application.util.DateUtils;
import com.findu.core.domain.model.constants.EstadoPerfil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class GestionPerfilClienteUseCaseImpl implements GestionPerfilClienteUseCase {

    private final PerfilClienteService perfilClienteService;
    private final DireccionService direccionService;

    @Override
    public PerfilClienteResponse crearPerfil(CrearPerfilClienteRequest request) {
        if (perfilClienteService.existsByAuthUserId(request.authUserId())) {
            throw new IllegalStateException("Ya existe un perfil de cliente para este usuario.");
        }
        if (perfilClienteService.existsByNumeroIdentificacion(request.numeroIdentificacion())) {
            throw new IllegalStateException("Ya existe un perfil con este número de identificación.");
        }

        DateUtils.validarMayorDeEdad(request.fechaNacimiento());

        PerfilCliente perfil = PerfilCliente.builder()
                .authUserId(request.authUserId())
                .username(request.username())
                .email(request.email())
                .nombreCompleto(request.nombreCompleto())
                .numeroIdentificacion(request.numeroIdentificacion())
                .tipoIdentificacion(request.tipoIdentificacion())
                .fechaNacimiento(request.fechaNacimiento())
                .sexo(request.sexo())
                .celular(request.celular())
                .codPhoneInternational(request.codPhoneInternational())
                .estado(EstadoPerfil.INCOMPLETO)
                .build();

        PerfilCliente saved = perfilClienteService.save(perfil);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PerfilClienteDetalleResponse consultarPerfil(Long id) {
        PerfilCliente perfil = perfilClienteService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de cliente no encontrado con id: " + id));

        List<DireccionResponse> direcciones = direccionService.findByClienteId(id).stream()
                .map(this::toDireccionResponse)
                .toList();

        return new PerfilClienteDetalleResponse(
                perfil.getId(),
                perfil.getUsername(),
                perfil.getEmail(),
                perfil.getNombreCompleto(),
                perfil.getNumeroIdentificacion(),
                perfil.getTipoIdentificacion(),
                perfil.getFechaNacimiento(),
                perfil.getSexo(),
                perfil.getCelular(),
                perfil.getCodPhoneInternational(),
                perfil.getUrlImagenPerfil(),
                perfil.getCalificacionPromedio(),
                perfil.getEstado(),
                direcciones
        );
    }

    @Override
    public PerfilClienteResponse actualizarPerfil(Long id, ActualizarPerfilClienteRequest request) {
        PerfilCliente perfil = perfilClienteService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de cliente no encontrado con id: " + id));

        if (!EstadoPerfil.INCOMPLETO.equals(perfil.getEstado())) {
            throw new IllegalStateException("Solo se puede actualizar el perfil mientras esté en estado INCOMPLETO. Estado actual: " + perfil.getEstado());
        }

        if (request.nombreCompleto() != null) {
            perfil.setNombreCompleto(request.nombreCompleto());
        }
        if (request.urlImagenPerfil() != null) {
            perfil.setUrlImagenPerfil(request.urlImagenPerfil());
        }

        PerfilCliente updated = perfilClienteService.save(perfil);
        return toResponse(updated);
    }

    @Override
    public void cambiarEstado(Long id, String nuevoEstado) {
        PerfilCliente perfil = perfilClienteService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de cliente no encontrado con id: " + id));
        perfil.setEstado(nuevoEstado);
        perfilClienteService.save(perfil);
    }

    private PerfilClienteResponse toResponse(PerfilCliente p) {
        return new PerfilClienteResponse(
                p.getId(), p.getUsername(), p.getEmail(), p.getNombreCompleto(), p.getCelular(),
                p.getCodPhoneInternational(), p.getSexo(), p.getUrlImagenPerfil(),
                p.getCalificacionPromedio(), p.getEstado()
        );
    }

    private DireccionResponse toDireccionResponse(Direccion d) {
        return new DireccionResponse(
                d.getId(), d.getEtiqueta(), d.getDireccionTexto(), null,
                d.getLatitud(), d.getLongitud(), d.getPiso(), d.getApartamento(),
                d.getReferencia(), d.isEsPrincipal()
        );
    }
}
