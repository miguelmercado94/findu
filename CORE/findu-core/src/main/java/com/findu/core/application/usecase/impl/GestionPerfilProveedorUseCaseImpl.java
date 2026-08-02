package com.findu.core.application.usecase.impl;

import com.findu.core.application.port.output.persistence.MunicipioRepositoryPort;
import com.findu.core.application.service.DireccionService;
import com.findu.core.application.service.PerfilEspecialistaService;
import com.findu.core.application.service.PerfilProveedorService;
import com.findu.core.application.service.PortafolioService;
import com.findu.core.application.service.ProveedorCoberturaService;
import com.findu.core.application.usecase.GestionPerfilProveedorUseCase;
import com.findu.core.domain.model.*;
import com.findu.core.dto.request.ActualizarPerfilProveedorRequest;
import com.findu.core.dto.request.CrearPerfilProveedorRequest;
import com.findu.core.dto.response.DireccionResponse;
import com.findu.core.dto.response.PerfilEspecialistaDetalleResponse;
import com.findu.core.dto.response.PerfilEspecialistaResponse;
import com.findu.core.dto.response.PerfilProveedorDetalleResponse;
import com.findu.core.dto.response.PerfilProveedorPublicoResponse;
import com.findu.core.dto.response.PerfilProveedorResponse;
import com.findu.core.dto.response.PortafolioItemResponse;
import com.findu.core.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class GestionPerfilProveedorUseCaseImpl implements GestionPerfilProveedorUseCase {

    private final PerfilProveedorService perfilProveedorService;
    private final PerfilEspecialistaService perfilEspecialistaService;
    private final DireccionService direccionService;
    private final PortafolioService portafolioService;
    private final ProveedorCoberturaService coberturaService;
    private final MunicipioRepositoryPort municipioPort;

    @Override
    public PerfilProveedorResponse crearPerfil(CrearPerfilProveedorRequest request) {
        if (perfilProveedorService.existsByAuthUserId(request.authUserId())) {
            throw new IllegalStateException("Ya existe un perfil de proveedor para este usuario.");
        }

        PerfilProveedor perfil = PerfilProveedor.builder()
                .authUserId(request.authUserId())
                .nombreCompleto(request.nombreCompleto())
                .numeroIdentificacion(request.numeroIdentificacion())
                .tipoIdentificacion(request.tipoIdentificacion())
                .fechaNacimiento(request.fechaNacimiento())
                .sexo(request.sexo())
                .celular(request.celular())
                .codPhoneInternational(request.codPhoneInternational())
                .estado("ACTIVO")
                .estadoVerificacion("PENDIENTE")
                .build();

        PerfilProveedor saved = perfilProveedorService.save(perfil);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PerfilProveedorDetalleResponse consultarPerfil(Long id) {
        PerfilProveedor perfil = perfilProveedorService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de proveedor no encontrado con id: " + id));

        List<PerfilEspecialistaResponse> especialidades = perfilEspecialistaService.findByProveedorId(id).stream()
                .map(e -> new PerfilEspecialistaResponse(e.getId(), null, e.getDescripcion(), e.getExperienciaAnios(), e.isActive()))
                .toList();

        List<DireccionResponse> direcciones = direccionService.findByProveedorId(id).stream()
                .map(d -> new DireccionResponse(d.getId(), d.getEtiqueta(), d.getDireccionTexto(), null,
                        d.getLatitud(), d.getLongitud(), d.getPiso(), d.getApartamento(), d.getReferencia(), d.isEsPrincipal()))
                .toList();

        return new PerfilProveedorDetalleResponse(
                perfil.getId(), perfil.getNombreCompleto(), perfil.getNumeroIdentificacion(),
                perfil.getTipoIdentificacion(), perfil.getFechaNacimiento(), perfil.getSexo(),
                perfil.getCelular(), perfil.getCodPhoneInternational(), perfil.getUrlImagenPerfil(),
                perfil.getCalificacionPromedio(), perfil.getEstado(), perfil.getEstadoVerificacion(),
                especialidades, direcciones
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PerfilProveedorPublicoResponse consultarPerfilPublico(Long proveedorId, Long servicioId) {
        PerfilProveedor perfil = perfilProveedorService.findById(proveedorId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de proveedor no encontrado con id: " + proveedorId));

        // Buscar la especialidad correspondiente al servicio solicitado
        PerfilEspecialista especialidad = perfilEspecialistaService.findByProveedorId(proveedorId).stream()
                .filter(e -> e.getServicioId().equals(servicioId) && e.isActive())
                .findFirst()
                .orElse(null);

        PerfilEspecialistaDetalleResponse especialidadResponse = null;
        List<PortafolioItemResponse> portafolioResponse = List.of();

        if (especialidad != null) {
            especialidadResponse = new PerfilEspecialistaDetalleResponse(
                    null,
                    especialidad.getDescripcion(),
                    especialidad.getExperienciaAnios()
            );

            portafolioResponse = portafolioService.findByEspecialistaId(especialidad.getId()).stream()
                    .map(p -> new PortafolioItemResponse(p.getId(), p.getTitulo(), p.getDescripcion(), p.getUrlImagen()))
                    .toList();
        }

        return new PerfilProveedorPublicoResponse(
                perfil.getNombreCompleto(),
                perfil.getNombreCompleto(),
                perfil.getUrlImagenPerfil(),
                perfil.getCalificacionPromedio(),
                especialidadResponse,
                portafolioResponse
        );
    }

    @Override
    public PerfilProveedorResponse actualizarPerfil(Long id, ActualizarPerfilProveedorRequest request) {
        PerfilProveedor perfil = perfilProveedorService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de proveedor no encontrado con id: " + id));

        if (request.nombreCompleto() != null) {
            perfil.setNombreCompleto(request.nombreCompleto());
        }
        if (request.urlImagenPerfil() != null) {
            perfil.setUrlImagenPerfil(request.urlImagenPerfil());
        }

        PerfilProveedor updated = perfilProveedorService.save(perfil);
        return toResponse(updated);
    }

    @Override
    public void cambiarEstado(Long id, String nuevoEstado) {
        PerfilProveedor perfil = perfilProveedorService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de proveedor no encontrado con id: " + id));
        perfil.setEstado(nuevoEstado);
        perfilProveedorService.save(perfil);
    }

    @Override
    public void actualizarCobertura(Long id, List<String> codigosDane) {
        perfilProveedorService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de proveedor no encontrado con id: " + id));

        List<Long> municipioIds = new ArrayList<>();
        for (String codigo : codigosDane) {
            Municipio m = municipioPort.findByCodigoDane(codigo)
                    .orElseThrow(() -> new ResourceNotFoundException("Municipio no encontrado con código DANE: " + codigo));
            municipioIds.add(m.getId());
        }

        coberturaService.replaceCobertura(id, municipioIds);
    }

    private PerfilProveedorResponse toResponse(PerfilProveedor p) {
        return new PerfilProveedorResponse(
                p.getId(), p.getNombreCompleto(), p.getCelular(), p.getCodPhoneInternational(),
                p.getSexo(), p.getUrlImagenPerfil(), p.getCalificacionPromedio(), p.getEstado(), p.getEstadoVerificacion()
        );
    }
}
