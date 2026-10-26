package com.findu.core.application.service;

import com.findu.core.application.port.out.externalapi.NotificationPort;
import com.findu.core.application.service.DireccionService;
import com.findu.core.application.service.OfertaService;
import com.findu.core.application.service.PerfilClienteService;
import com.findu.core.application.service.PerfilProveedorService;
import com.findu.core.application.service.SolicitudServicioService;
import com.findu.core.application.port.in.GestionOfertasUseCase;
import com.findu.core.domain.model.Direccion;
import com.findu.core.domain.model.Oferta;
import com.findu.core.domain.model.PerfilCliente;
import com.findu.core.domain.model.PerfilProveedor;
import com.findu.core.application.port.out.persistence.CategoriaRepositoryPort;
import com.findu.core.application.port.out.persistence.MunicipioRepositoryPort;
import com.findu.core.application.port.out.persistence.ServicioRepositoryPort;
import com.findu.core.domain.enums.NotificationTemplates;
import com.findu.core.domain.model.*;
import com.findu.core.domain.observer.SolicitudEventPublisher;
import com.findu.core.application.dto.request.CrearOfertaRequest;
import com.findu.core.application.dto.response.OfertaResponse;
import com.findu.core.application.dto.response.SolicitudResponse;
import com.findu.core.domain.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class GestionOfertasUseCaseImpl implements GestionOfertasUseCase {

    private final OfertaService ofertaService;
    private final SolicitudServicioService solicitudService;
    private final PerfilProveedorService proveedorService;
    private final PerfilClienteService perfilClienteService;
    private final DireccionService direccionService;
    private final NotificationPort notificationPort;
    private final SolicitudEventPublisher eventPublisher;
    private final ServicioRepositoryPort servicioPort;
    private final PerfilEspecialistaService perfilEspecialistaService;
    private final ProveedorCoberturaService proveedorCoberturaService;
    private final CategoriaRepositoryPort categoriaPort;
    private final MunicipioRepositoryPort municipioPort;
    private final CalificacionService calificacionService;

    @Override
    public OfertaResponse enviarOferta(CrearOfertaRequest request) {
        SolicitudServicio solicitud = solicitudService.findById(request.solicitudServicioId())
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada"));

        String estadoActual = solicitud.getEstadoSolicitud();
        if (estadoActual != null && !"ABIERTA".equalsIgnoreCase(estadoActual) && !"EN_NEGOCIACION".equalsIgnoreCase(estadoActual)) {
            throw new IllegalStateException("No se pueden enviar ofertas a una solicitud en estado: " + estadoActual);
        }

        if (ofertaService.existsBySolicitudIdAndProveedorId(request.solicitudServicioId(), request.perfilProveedorId())) {
            throw new IllegalStateException("Ya enviaste una oferta para esta solicitud.");
        }

        if (request.valorPropuesto() == null && request.horaInicioPropuesta() == null) {
            throw new IllegalArgumentException("Debe proponer al menos un precio o una hora de inicio.");
        }

        BigDecimal valorPropuesto = request.valorPropuesto();
        if (valorPropuesto == null) {
            valorPropuesto = solicitud.getPresupuestoMaximo();
        }
        if (valorPropuesto == null) {
            throw new IllegalArgumentException("Debe especificar un valor propuesto ya que la solicitud no tiene presupuesto asignado.");
        }
        if (valorPropuesto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El valor propuesto debe ser mayor a cero.");
        }

        if (Boolean.TRUE.equals(solicitud.getEsPresupuestoEstricto()) &&
                solicitud.getPresupuestoMaximo() != null &&
                valorPropuesto.compareTo(solicitud.getPresupuestoMaximo()) > 0) {
            throw new IllegalStateException("El cliente ha fijado un tope máximo estricto ($" + solicitud.getPresupuestoMaximo() + "). Tu oferta ($" + valorPropuesto + ") excede este límite.");
        }

        if (request.horaInicioPropuesta() != null && solicitud.getFechaProgramada() != null) {
            java.time.LocalDateTime fechaHoraPropuesta = solicitud.getFechaProgramada().toLocalDate().atTime(request.horaInicioPropuesta());
            if (fechaHoraPropuesta.isBefore(java.time.LocalDateTime.now())) {
                throw new IllegalArgumentException("La hora propuesta (" + request.horaInicioPropuesta() + ") resulta en una fecha/hora en el pasado.");
            }
        }

        Oferta oferta = Oferta.builder()
                .solicitudServicioId(request.solicitudServicioId())
                .perfilProveedorId(request.perfilProveedorId())
                .valorPropuesto(valorPropuesto)
                .horaInicioPropuesta(request.horaInicioPropuesta())
                .tiempoEstimado(request.tiempoEstimado())
                .mensajePresentacion(request.mensajePresentacion())
                .estadoOferta("ENVIADA")
                .build();

        // Delegate state transition logic to domain entity via State Pattern
        solicitud.recibirOferta(oferta);
        solicitudService.save(solicitud);

        Oferta saved = ofertaService.save(oferta);

        // Notify client and observers
        PerfilProveedor proveedor = proveedorService.findById(request.perfilProveedorId()).orElse(null);
        PerfilCliente cliente = perfilClienteService.findById(solicitud.getPerfilClienteId()).orElse(null);

        eventPublisher.notifyOfertaRecibida(solicitud, saved, cliente, proveedor);

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OfertaResponse> listarOfertasPorSolicitud(Long solicitudId) {
        return ofertaService.findBySolicitudId(solicitudId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public OfertaResponse aceptarOferta(Long ofertaId) {
        Oferta oferta = ofertaService.findById(ofertaId)
                .orElseThrow(() -> new ResourceNotFoundException("Oferta no encontrada con id: " + ofertaId));

        if (!"ENVIADA".equals(oferta.getEstadoOferta())) {
            throw new IllegalStateException("Solo se pueden aceptar ofertas en estado ENVIADA.");
        }

        SolicitudServicio solicitud = solicitudService.findById(oferta.getSolicitudServicioId())
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada"));

        // Delegate state transition via State Pattern
        solicitud.aceptarOferta(oferta);

        // Accept this offer
        oferta.setEstadoOferta("ACEPTADA");
        ofertaService.save(oferta);

        // Reject all other offers for the same solicitud
        List<Oferta> otrasOfertas = ofertaService.findBySolicitudId(oferta.getSolicitudServicioId()).stream()
                .filter(o -> !o.getId().equals(ofertaId))
                .peek(o -> o.setEstadoOferta("RECHAZADA"))
                .toList();
        if (!otrasOfertas.isEmpty()) {
            ofertaService.saveAll(otrasOfertas);
        }

        solicitudService.save(solicitud);

        // Notify observers (Winning provider gets notification, all other eligible/offering providers get SOLICITUD_CERRADA dissipation event)
        PerfilProveedor proveedorAceptado = proveedorService.findById(oferta.getPerfilProveedorId()).orElse(null);
        Long municipioId = null;
        if (solicitud.getDireccionId() != null) {
            municipioId = direccionService.findById(solicitud.getDireccionId())
                    .map(Direccion::getMunicipioId)
                    .orElse(null);
        }
        List<PerfilProveedor> proveedoresNotificados = proveedorService.findEligibleProviders(solicitud.getServicioId(), municipioId);
        eventPublisher.notifyOfertaAceptada(solicitud, oferta, otrasOfertas, proveedorAceptado, proveedoresNotificados);

        return toResponse(oferta);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SolicitudResponse> obtenerSolicitudesDisponibles(Long proveedorId) {
        proveedorService.findById(proveedorId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de proveedor no encontrado con id: " + proveedorId));

        // 1. Obtener especialidades activas del proveedor
        List<PerfilEspecialista> especialidades = perfilEspecialistaService.findByProveedorId(proveedorId).stream()
                .filter(PerfilEspecialista::isActive)
                .toList();

        if (especialidades.isEmpty()) {
            return List.of();
        }

        Set<Long> serviciosHabilitados = especialidades.stream()
                .map(PerfilEspecialista::getServicioId)
                .collect(Collectors.toSet());

        // 2. Obtener cobertura del proveedor (si no tiene cobertura configurada, aplica cobertura total/nacional)
        List<ProveedorCobertura> coberturas = proveedorCoberturaService.findByProveedorId(proveedorId);
        Set<Long> municipiosCobertura = coberturas.stream()
                .map(ProveedorCobertura::getMunicipioId)
                .collect(Collectors.toSet());
        boolean coberturaTotal = municipiosCobertura.isEmpty();

        // 3. Filtrar solicitudes ABIERTAS que correspondan a su especialidad y cobertura, y donde aún no haya ofertado
        return solicitudService.findByEstado("ABIERTA").stream()
                .filter(s -> s.getServicioId() != null && serviciosHabilitados.contains(s.getServicioId()))
                .filter(s -> {
                    if (coberturaTotal) return true;
                    if (s.getDireccionId() == null) return true;
                    return direccionService.findById(s.getDireccionId())
                            .map(d -> d.getMunicipioId() == null || municipiosCobertura.contains(d.getMunicipioId()))
                            .orElse(true);
                })
                .filter(s -> !ofertaService.existsBySolicitudIdAndProveedorId(s.getId(), proveedorId))
                .map(this::mapToDisponibleResponse)
                .toList();
    }

    private SolicitudResponse mapToDisponibleResponse(SolicitudServicio s) {
        String servicioNombre = null;
        String categoriaNombre = null;
        if (s.getServicioId() != null) {
            Servicio serv = servicioPort.findById(s.getServicioId()).orElse(null);
            if (serv != null) {
                servicioNombre = serv.getNombre();
                if (serv.getCategoriaId() != null) {
                    categoriaNombre = categoriaPort.findById(serv.getCategoriaId())
                            .map(Categoria::getNombre)
                            .orElse(null);
                }
            }
        }

        String direccionTexto = null;
        String municipioNombre = null;
        String codigoDane = null;
        if (s.getDireccionId() != null) {
            Direccion dir = direccionService.findById(s.getDireccionId()).orElse(null);
            if (dir != null) {
                direccionTexto = dir.getDireccionTexto();
                if (dir.getMunicipioId() != null) {
                    Municipio mun = municipioPort.findById(dir.getMunicipioId()).orElse(null);
                    if (mun != null) {
                        municipioNombre = mun.getNombre();
                        codigoDane = mun.getCodigoDane();
                    }
                }
            }
        }

        String nombreContacto = s.getNombreContacto();
        BigDecimal clienteCalificacion = BigDecimal.valueOf(5.0);
        if (s.getPerfilClienteId() != null) {
            PerfilCliente cliente = perfilClienteService.findById(s.getPerfilClienteId()).orElse(null);
            if (cliente != null) {
                if (nombreContacto == null || nombreContacto.isBlank()) {
                    nombreContacto = cliente.getNombreCompleto();
                }
                if (cliente.getCalificacionPromedio() != null) {
                    clienteCalificacion = cliente.getCalificacionPromedio();
                }
            }
        }

        return new SolicitudResponse(
                s.getId(),
                servicioNombre,
                categoriaNombre,
                direccionTexto,
                municipioNombre,
                codigoDane,
                2.5, // Distancia estimada de proximidad en km
                s.getFechaProgramada(),
                nombreContacto,
                null, // Teléfono oculto por privacidad en solicitudes abiertas
                clienteCalificacion,
                s.getPrioridad(),
                s.getPresupuestoMaximo(),
                s.getEsPresupuestoEstricto(),
                null, // tarifaAcordada aún no definida
                s.getDetalles(),
                s.getFotos(),
                s.getEstadoSolicitud(),
                null, // calificacionRecibida
                null  // resenaCliente
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<SolicitudResponse> obtenerMisSolicitudes(Long proveedorId, String estado) {
        proveedorService.findById(proveedorId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de proveedor no encontrado con id: " + proveedorId));

        String estadoFiltro = (estado != null && !estado.isBlank()) ? estado.toUpperCase() : null;

        return solicitudService.findByProveedorIdViaOfertaAceptada(proveedorId, estadoFiltro).stream()
                .map(s -> {
                    String servicioNombre = null;
                    String categoriaNombre = null;
                    if (s.getServicioId() != null) {
                        Servicio serv = servicioPort.findById(s.getServicioId()).orElse(null);
                        if (serv != null) {
                            servicioNombre = serv.getNombre();
                            if (serv.getCategoriaId() != null) {
                                categoriaNombre = categoriaPort.findById(serv.getCategoriaId())
                                        .map(Categoria::getNombre)
                                        .orElse(null);
                            }
                        }
                    }

                    String direccionTexto = null;
                    String municipioNombre = null;
                    String codigoDane = null;
                    if (s.getDireccionId() != null) {
                        Direccion dir = direccionService.findById(s.getDireccionId()).orElse(null);
                        if (dir != null) {
                            direccionTexto = dir.getDireccionTexto();
                            if (dir.getMunicipioId() != null) {
                                Municipio mun = municipioPort.findById(dir.getMunicipioId()).orElse(null);
                                if (mun != null) {
                                    municipioNombre = mun.getNombre();
                                    codigoDane = mun.getCodigoDane();
                                }
                            }
                        }
                    }

                    BigDecimal tarifaAcordada = ofertaService.findBySolicitudId(s.getId()).stream()
                            .filter(o -> proveedorId.equals(o.getPerfilProveedorId()) && "ACEPTADA".equals(o.getEstadoOferta()))
                            .map(Oferta::getValorPropuesto)
                            .findFirst()
                            .orElse(s.getPresupuestoMaximo());

                    BigDecimal calificacionRecibida = null;
                    String resenaCliente = null;
                    List<Calificacion> calificaciones = calificacionService.findByEvaluadoId(proveedorId);
                    for (Calificacion c : calificaciones) {
                        if (s.getId().equals(c.getSolicitudServicioId())) {
                            calificacionRecibida = c.getPuntaje() != null ? BigDecimal.valueOf(c.getPuntaje()) : null;
                            resenaCliente = c.getComentario();
                            break;
                        }
                    }

                    String nombreContacto = s.getNombreContacto();
                    String telefonoContacto = s.getTelefonoContacto();
                    BigDecimal clienteCalificacion = BigDecimal.valueOf(5.0);
                    if (s.getPerfilClienteId() != null) {
                        PerfilCliente cliente = perfilClienteService.findById(s.getPerfilClienteId()).orElse(null);
                        if (cliente != null) {
                            if (nombreContacto == null || nombreContacto.isBlank()) {
                                nombreContacto = cliente.getNombreCompleto();
                            }
                            if (telefonoContacto == null || telefonoContacto.isBlank()) {
                                telefonoContacto = cliente.getCelular();
                            }
                            if (cliente.getCalificacionPromedio() != null) {
                                clienteCalificacion = cliente.getCalificacionPromedio();
                            }
                        }
                    }

                    return new SolicitudResponse(
                            s.getId(),
                            servicioNombre,
                            categoriaNombre,
                            direccionTexto,
                            municipioNombre,
                            codigoDane,
                            0.0,
                            s.getFechaProgramada(),
                            nombreContacto,
                            telefonoContacto,
                            clienteCalificacion,
                            s.getPrioridad(),
                            s.getPresupuestoMaximo(),
                            s.getEsPresupuestoEstricto(),
                            tarifaAcordada,
                            s.getDetalles(),
                            s.getFotos(),
                            s.getEstadoSolicitud(),
                            calificacionRecibida,
                            resenaCliente
                    );
                })
                .toList();
    }

    private OfertaResponse toResponse(Oferta o) {
        PerfilProveedor proveedor = proveedorService.findById(o.getPerfilProveedorId()).orElse(null);
        String nombreProveedor = proveedor != null ? proveedor.getNombreCompleto() : "Proveedor FINDU";
        String fotoProveedor = proveedor != null ? proveedor.getUrlImagenPerfil() : null;
        var calificacion = proveedor != null ? proveedor.getCalificacionPromedio() : null;

        return new OfertaResponse(
                o.getId(), o.getPerfilProveedorId(), nombreProveedor, fotoProveedor, calificacion,
                o.getValorPropuesto(), o.getHoraInicioPropuesta(), o.getTiempoEstimado(),
                o.getMensajePresentacion(), o.getEstadoOferta()
        );
    }
}
