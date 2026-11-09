package com.findu.core.application.service;

import com.findu.core.application.port.out.persistence.MunicipioRepositoryPort;
import com.findu.core.application.service.DireccionService;
import com.findu.core.application.service.PerfilEspecialistaService;
import com.findu.core.application.service.PerfilProveedorService;
import com.findu.core.application.service.PortafolioService;
import com.findu.core.application.service.ProveedorCoberturaService;
import com.findu.core.application.port.in.GestionPerfilProveedorUseCase;
import com.findu.core.domain.model.*;
import com.findu.core.application.dto.request.ActualizarPerfilProveedorRequest;
import com.findu.core.application.dto.request.CrearPerfilProveedorRequest;
import com.findu.core.application.dto.response.DireccionResponse;
import com.findu.core.application.dto.response.EspecialistaCredencialResponse;
import com.findu.core.application.dto.response.MunicipioResponse;
import com.findu.core.application.dto.response.PerfilEspecialistaDetalleResponse;
import com.findu.core.application.dto.response.PerfilEspecialistaResponse;
import com.findu.core.application.dto.response.PerfilProveedorDetalleResponse;
import com.findu.core.application.dto.response.PerfilProveedorPublicoResponse;
import com.findu.core.application.dto.response.PerfilProveedorResponse;
import com.findu.core.application.dto.response.PortafolioItemResponse;
import com.findu.core.domain.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final com.findu.core.application.port.out.persistence.ServicioRepositoryPort servicioPort;
    private final FotoUrlResolver fotoUrlResolver;
    private final com.findu.core.application.port.out.storage.ObjectStoragePort storage;

    @org.springframework.beans.factory.annotation.Value("${findu.notification.processor.base-url:http://findu-notification-processor.railway.internal:8083}")
    private String notificationProcessorBaseUrl;

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
                .calificacionPromedio(new BigDecimal("3.5"))
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

        List<MunicipioResponse> cobertura = consultarCobertura(id);

        return new PerfilProveedorDetalleResponse(
                perfil.getId(), perfil.getNombreCompleto(), perfil.getNumeroIdentificacion(),
                perfil.getTipoIdentificacion(), perfil.getFechaNacimiento(), perfil.getSexo(),
                perfil.getCelular(), perfil.getCodPhoneInternational(), fotoUrlResolver.resolve(perfil),
                calcularCalificacionGeneral(especialidades), perfil.getEstado(), perfil.getEstadoVerificacion(),
                especialidades, direcciones, cobertura, perfil.isDisponible(),
                evidenciaBiometricaOk(perfil)
        );
    }

    /** true solo si el espejo esta marcado Y rostro.png existe; un fallo de S3 cuenta como false (la app ofrece repetir la biometria). */
    private boolean evidenciaBiometricaOk(PerfilProveedor perfil) {
        if (perfil.getBiometriaVerificadaAt() == null) {
            return false;
        }
        try {
            return storage.exists(ProviderStoragePaths.rostroKey(perfil.getTipoIdentificacion(), perfil.getNumeroIdentificacion()));
        } catch (RuntimeException e) {
            log.warn("No se pudo verificar la evidencia biometrica perfilId={}: {}", perfil.getId(), e.toString());
            return false;
        }
    }

    /**
     * Calificacion general del perfil = promedio de las calificaciones de las especialidades activas.
     * La lista recibida ya corresponde a las especialidades activas del proveedor. Se promedian los
     * valores no nulos y se redondea a 1 decimal (HALF_UP). Si no hay valores aplicables, se devuelve
     * el piso de negocio 3.5.
     */
    private BigDecimal calcularCalificacionGeneral(List<PerfilEspecialistaResponse> especialidades) {
        List<BigDecimal> calificaciones = especialidades.stream()
                .map(PerfilEspecialistaResponse::calificacionPromedio)
                .filter(java.util.Objects::nonNull)
                .toList();

        if (calificaciones.isEmpty()) {
            return new BigDecimal("3.5").setScale(1, RoundingMode.HALF_UP);
        }

        BigDecimal suma = calificaciones.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return suma.divide(new BigDecimal(calificaciones.size()), 1, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MunicipioResponse> consultarCobertura(Long id) {
        perfilProveedorService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de proveedor no encontrado con id: " + id));

        return coberturaService.findByProveedorId(id).stream()
                .map(c -> municipioPort.findById(c.getMunicipioId()).orElse(null))
                .filter(java.util.Objects::nonNull)
                .map(m -> new MunicipioResponse(m.getId(), m.getCodigoDane(), m.getNombre(), m.getDepartamento()))
                .toList();
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
                fotoUrlResolver.resolve(perfil),
                especialidad != null && especialidad.getCalificacionPromedio() != null ? especialidad.getCalificacionPromedio() : perfil.getCalificacionPromedio(),
                especialidadResponse,
                portafolioResponse
        );
    }

    @Override
    public PerfilProveedorResponse actualizarPerfil(Long id, ActualizarPerfilProveedorRequest request) {
        PerfilProveedor perfil = perfilProveedorService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de proveedor no encontrado con id: " + id));

        assertDatosEditables(perfil, request);
        if (request.nombreCompleto() != null) {
            perfil.setNombreCompleto(request.nombreCompleto());
        }
        if (request.fechaNacimiento() != null) {
            perfil.setFechaNacimiento(request.fechaNacimiento());
        }
        if (perfil.getUrlImagenPerfil() == null && esFotoAceptable(perfil, request.urlImagenPerfil())) {
            perfil.setUrlImagenPerfil(request.urlImagenPerfil());
        }

        PerfilProveedor updated = perfilProveedorService.save(perfil);
        return toResponse(updated);
    }

    /**
     * Unico punto de la regla de datos personales. Con biometria completa (o perfil ya VERIFICADO, guarda
     * previa) un nombre o fecha de nacimiento DISTINTOS al actual (o enviados cuando el actual es nulo) se rechazan con
     * 409; valores iguales no son cambio. Sin biometria se valida que la fecha no sea futura (400).
     */
    private void assertDatosEditables(PerfilProveedor perfil, ActualizarPerfilProveedorRequest request) {
        boolean verificado = GestionArchivosProveedorUseCaseImpl.biometriaCompleta(perfil)
                || "VERIFICADO".equalsIgnoreCase(perfil.getEstadoVerificacion());

        // Con valor actual nulo/blanco, cualquier valor enviado tambien es un cambio.
        boolean nombreCambia = request.nombreCompleto() != null
                && (perfil.getNombreCompleto() == null || perfil.getNombreCompleto().isBlank()
                || !perfil.getNombreCompleto().trim().equals(request.nombreCompleto().trim()));
        boolean fechaCambia = request.fechaNacimiento() != null
                && !request.fechaNacimiento().equals(perfil.getFechaNacimiento());

        if (verificado) {
            if (nombreCambia || fechaCambia) {
                throw new IllegalStateException("Los datos verificados no se pueden modificar; abre un ticket de soporte");
            }
            return;
        }
        if (request.fechaNacimiento() != null && request.fechaNacimiento().isAfter(java.time.LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de nacimiento no puede ser futura");
        }
    }

    /**
     * D10: solo se acepta una clave del propio proveedor (proveedores/{TIPODOC}{NUM}/...) o una URL http(s).
     * Cualquier otro valor (URL relativa del endpoint, base64, rutas locales) se ignora.
     */
    private boolean esFotoAceptable(PerfilProveedor perfil, String valor) {
        if (valor == null || valor.isBlank()) {
            return false;
        }
        String v = valor.trim().toLowerCase();
        if (v.startsWith("http://") || v.startsWith("https://")) {
            return true;
        }
        if (FotoUrlResolver.isStorageKey(valor.trim())) {
            try {
                // Solo bajo perfil/: biometrico/* y especializaciones/* nunca son foto de perfil.
                return valor.trim().startsWith(ProviderStoragePaths.prefix(
                        perfil.getTipoIdentificacion(), perfil.getNumeroIdentificacion()) + "perfil/");
            } catch (com.findu.core.domain.exception.InvalidProviderDocumentException e) {
                return false;
            }
        }
        return false;
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
            String url = notificationProcessorBaseUrl + "/api/v1/canales/proveedor/disponibilidad";
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
                p.getSexo(), fotoUrlResolver.resolve(p), p.getCalificacionPromedio(), p.getEstado(), p.getEstadoVerificacion(),
                p.isDisponible()
        );
    }
}
