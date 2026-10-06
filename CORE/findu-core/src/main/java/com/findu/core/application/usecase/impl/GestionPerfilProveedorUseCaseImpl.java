package com.findu.core.application.usecase.impl;

import com.findu.core.application.port.output.persistence.MunicipioRepositoryPort;
import com.findu.core.application.service.*;
import com.findu.core.application.usecase.GestionPerfilProveedorUseCase;
import com.findu.core.domain.model.*;
import com.findu.core.dto.request.ActualizarPerfilProveedorRequest;
import com.findu.core.dto.request.CrearPerfilProveedorRequest;
import com.findu.core.dto.response.*;
import com.findu.core.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GestionPerfilProveedorUseCaseImpl implements GestionPerfilProveedorUseCase {

    private final PerfilProveedorService perfilProveedorService;
    private final PerfilEspecialistaService perfilEspecialistaService;
    private final DireccionService direccionService;
    private final PortafolioService portafolioService;
    private final EspecialistaCredencialService credencialService;
    private final ProveedorCoberturaService coberturaService;
    private final MunicipioRepositoryPort municipioPort;
    private final com.findu.core.application.port.output.persistence.ServicioRepositoryPort servicioPort;

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

        // Mapa servicioId -> nombre para resolver el nombre real del servicio de cada especialidad.
        java.util.Map<Long, String> nombresServicio = servicioPort.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(Servicio::getId, Servicio::getNombre, (a, b) -> a));

        List<PerfilEspecialistaResponse> especialidades = perfilEspecialistaService.findByProveedorId(id).stream()
                .map(e -> new PerfilEspecialistaResponse(
                        e.getId(),
                        e.getServicioId(),
                        nombresServicio.getOrDefault(e.getServicioId(), null),
                        e.getDescripcion(),
                        e.getExperienciaAnios(),
                        e.getCalificacionPromedio(),
                        e.isActive()))
                .toList();

        List<DireccionResponse> direcciones = direccionService.findByProveedorId(id).stream()
                .map(d -> new DireccionResponse(d.getId(), d.getEtiqueta(), d.getDireccionTexto(), null,
                        d.getLatitud(), d.getLongitud(), d.getPiso(), d.getApartamento(), d.getReferencia(), d.isEsPrincipal()))
                .toList();

        List<MunicipioResponse> cobertura = coberturaService.findByProveedorId(id).stream()
                .map(c -> municipioPort.findById(c.getMunicipioId()).orElse(null))
                .filter(java.util.Objects::nonNull)
                .map(m -> new MunicipioResponse(m.getId(), m.getCodigoDane(), m.getNombre(), m.getDepartamento()))
                .toList();

        return new PerfilProveedorDetalleResponse(
                perfil.getId(), perfil.getNombreCompleto(), perfil.getNumeroIdentificacion(),
                perfil.getTipoIdentificacion(), perfil.getFechaNacimiento(), perfil.getSexo(),
                perfil.getCelular(), perfil.getCodPhoneInternational(), perfil.getUrlImagenPerfil(),
                perfil.getCalificacionPromedio(), perfil.getEstado(), perfil.getEstadoVerificacion(),
                especialidades, direcciones, cobertura, perfil.isDisponible()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PerfilProveedorDetalleResponse consultarPerfilPorAuthUserId(Long authUserId) {
        PerfilProveedor perfil = perfilProveedorService.findByAuthUserId(authUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de proveedor no encontrado para authUserId: " + authUserId));
        return consultarPerfil(perfil.getId());
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
            List<EspecialistaCredencialResponse> credencialesResponse = credencialService.findByEspecialistaId(especialidad.getId()).stream()
                    .map(c -> new EspecialistaCredencialResponse(
                            c.getId(), c.getPerfilEspecialistaId(), c.getTipoCertificado(),
                            c.getNombreTitulo(), c.getInstitucion(), c.getFechaInicio(),
                            c.getFechaFin(), c.getUrlCertificadoS3()))
                    .toList();

            String servNombre = servicioPort.findAll().stream()
                    .filter(s -> s.getId().equals(especialidad.getServicioId()))
                    .map(Servicio::getNombre)
                    .findFirst()
                    .orElse(null);

            especialidadResponse = new PerfilEspecialistaDetalleResponse(
                    servNombre,
                    especialidad.getDescripcion(),
                    especialidad.getExperienciaAnios(),
                    credencialesResponse
            );

            portafolioResponse = portafolioService.findByEspecialistaId(especialidad.getId()).stream()
                    .map(p -> new PortafolioItemResponse(p.getId(), p.getTitulo(), p.getDescripcion(), p.getUrlImagen(), p.getUrlFolderImagen()))
                    .toList();
        }

        return new PerfilProveedorPublicoResponse(
                perfil.getNombreCompleto(),
                perfil.getNombreCompleto(),
                perfil.getUrlImagenPerfil(),
                especialidad != null && especialidad.getCalificacionPromedio() != null ? especialidad.getCalificacionPromedio() : perfil.getCalificacionPromedio(),
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

    @Override
    public PerfilProveedorResponse actualizarDisponibilidad(Long id, boolean disponible) {
        PerfilProveedor perfil = perfilProveedorService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de proveedor no encontrado con id: " + id));

        perfil.setDisponible(disponible);
        PerfilProveedor saved = perfilProveedorService.save(perfil);

        notificarDispatcherDisponibilidad(id, disponible);

        return toResponse(saved);
    }

    private void notificarDispatcherDisponibilidad(Long proveedorId, boolean disponible) {
        try {
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            String url = "http://findu-notification-processor:8083/api/v1/canales/proveedor/disponibilidad";
            java.util.Map<String, Object> req = java.util.Map.of("proveedorId", proveedorId, "disponible", disponible);
            restTemplate.put(url, req);
            log.info("Canal en dispatcher actualizado exitosamente para proveedor {}: disponible={}", proveedorId, disponible);
        } catch (Exception e) {
            log.warn("No se pudo notificar al dispatcher sobre cambio de disponibilidad del proveedor {}: {}", proveedorId, e.getMessage());
        }
    }

    private PerfilProveedorResponse toResponse(PerfilProveedor p) {
        return new PerfilProveedorResponse(
                p.getId(), p.getNombreCompleto(), p.getCelular(), p.getCodPhoneInternational(),
                p.getSexo(), p.getUrlImagenPerfil(), p.getCalificacionPromedio(), p.getEstado(), p.getEstadoVerificacion(),
                p.isDisponible()
        );
    }
}
